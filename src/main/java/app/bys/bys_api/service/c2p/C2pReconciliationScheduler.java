package app.bys.bys_api.service.c2p;

import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pMovement;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pMovementsResponse;
import app.bys.bys_api.model.entity.C2pAttempt;
import app.bys.bys_api.model.enums.C2pAttemptStatus;
import app.bys.bys_api.repository.C2pAttemptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resuelve los cobros C2P que quedaron sin respuesta (UNKNOWN) o interrumpidos (PROCESSING):
 * busca el cobro en c2p/mov de Bancamiga y, si aparece aprobado, registra el pago.
 * Si no aparece despues de bancamiga.c2p.reconcile.give-up-minutes, el intento se marca ERROR (no se cobro).
 * Si Bancamiga no responde por mas de 24 h, el intento pasa a REVIEW para revision manual.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class C2pReconciliationScheduler {

    private static final double AMOUNT_TOLERANCE = 0.01;
    private static final int MIN_AGE_MINUTES = 2;
    private static final int REVIEW_AFTER_HOURS = 24;

    private final C2pAttemptRepository attemptRepository;
    private final BancamigaC2pClient c2pClient;
    private final C2pPaymentService c2pPaymentService;

    @Value("${bancamiga.c2p.reconcile.give-up-minutes}")
    private int giveUpMinutes;

    @Scheduled(initialDelay = 60000, fixedDelayString = "${bancamiga.c2p.reconcile.fixed-delay-ms}")
    public void reconcile() {
        if (!c2pPaymentService.isEnabled()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        List<C2pAttempt> pending = attemptRepository.findByStatusInAndCreatedAtBefore(
                List.of(C2pAttemptStatus.UNKNOWN, C2pAttemptStatus.PROCESSING), now.minusMinutes(MIN_AGE_MINUTES));
        if (pending.isEmpty()) {
            return;
        }
        log.debug("Conciliando {} cobro(s) C2P pendiente(s)", pending.size());

        // Movimientos por fecha, consultados una sola vez por corrida. Empty = la consulta fallo.
        Map<LocalDate, Optional<List<BancamigaC2pMovement>>> movementsByDate = new HashMap<>();
        Set<String> usedRefs = new HashSet<>();

        for (C2pAttempt attempt : pending) {
            try {
                reconcileOne(attempt, now, movementsByDate, usedRefs);
            } catch (RuntimeException e) {
                log.warn("Error conciliando el cobro C2P {}: {}", attempt.getId(), e.getMessage());
            }
        }
    }

    private void reconcileOne(C2pAttempt attempt, LocalDateTime now,
                              Map<LocalDate, Optional<List<BancamigaC2pMovement>>> movementsByDate, Set<String> usedRefs) {
        if (attempt.getPhone() == null || attempt.getIdDocument() == null) {
            c2pPaymentService.updateStatus(attempt.getId(), C2pAttemptStatus.REVIEW, null,
                    "Sin datos del pagador para conciliar (cuenta eliminada)");
            return;
        }

        // El cobro pudo quedar registrado el mismo dia o, cerca de la medianoche, el dia siguiente
        List<LocalDate> dates = new ArrayList<>();
        dates.add(attempt.getCreatedAt().toLocalDate());
        LocalDate nextDay = attempt.getCreatedAt().plusMinutes(10).toLocalDate();
        if (!nextDay.equals(dates.get(0))) {
            dates.add(nextDay);
        }

        boolean allQueriesOk = true;
        for (LocalDate date : dates) {
            Optional<List<BancamigaC2pMovement>> movements =
                    movementsByDate.computeIfAbsent(date, this::fetchMovements);
            if (movements.isEmpty()) {
                allQueriesOk = false;
                continue;
            }
            Optional<BancamigaC2pMovement> match = movements.get().stream()
                    .filter(m -> matches(attempt, m))
                    .filter(m -> m.getNroref() == null || !usedRefs.contains(m.getNroref()))
                    .filter(m -> m.getNroref() == null
                            || !attemptRepository.existsByBancamigaNrorefAndStatus(m.getNroref(), C2pAttemptStatus.APPROVED))
                    .findFirst();
            if (match.isPresent()) {
                BancamigaC2pMovement movement = match.get();
                if (movement.getNroref() != null) {
                    usedRefs.add(movement.getNroref());
                }
                c2pPaymentService.approve(attempt.getId(), movement.getNroref(), movement.getDataorig(), movement.getId(),
                        "C2P_RECONCILED", null);
                return;
            }
        }

        if (allQueriesOk && attempt.getCreatedAt().isBefore(now.minusMinutes(giveUpMinutes))) {
            c2pPaymentService.updateStatus(attempt.getId(), C2pAttemptStatus.ERROR, null,
                    "El cobro no aparece en Bancamiga (c2p/mov): no se realizo");
            log.info("Cobro C2P {} sin registro en Bancamiga tras {} min, se marca ERROR", attempt.getId(), giveUpMinutes);
        } else if (!allQueriesOk && attempt.getCreatedAt().isBefore(now.minusHours(REVIEW_AFTER_HOURS))) {
            c2pPaymentService.updateStatus(attempt.getId(), C2pAttemptStatus.REVIEW, null,
                    "No se pudo conciliar con Bancamiga en 24 h; revisar manualmente");
            log.error("Cobro C2P {} sin conciliar tras 24 h, pasa a REVIEW", attempt.getId());
        }
    }

    private Optional<List<BancamigaC2pMovement>> fetchMovements(LocalDate date) {
        try {
            BancamigaC2pMovementsResponse response = c2pClient.movements(date);
            List<BancamigaC2pMovement> list = response != null && response.getLista() != null ? response.getLista() : List.of();
            return Optional.of(list);
        } catch (RuntimeException e) {
            log.warn("No se pudo consultar c2p/mov de Bancamiga para {}: {}", date, e.getMessage());
            return Optional.empty();
        }
    }

    private boolean matches(C2pAttempt attempt, BancamigaC2pMovement movement) {
        if (!C2pPaymentService.isApproved(movement.getStatus())) {
            return false;
        }
        if (movement.getAmount() == null
                || Math.abs(movement.getAmount() - attempt.getAmountBs().doubleValue()) > AMOUNT_TOLERANCE) {
            return false;
        }
        if (!attempt.getPhone().equals(movement.getPhoneorig())) {
            return false;
        }
        return movement.getDni() != null && attempt.getIdDocument().equalsIgnoreCase(movement.getDni().trim());
    }
}
