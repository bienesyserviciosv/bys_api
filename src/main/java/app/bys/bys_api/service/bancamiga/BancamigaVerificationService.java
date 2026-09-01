package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.error.ServiceRequestAlreadyAcceptedException;
import app.bys.bys_api.model.dto.bancamiga.BancamigaFindPaymentResponse;
import app.bys.bys_api.model.dto.bancamiga.BancamigaMovementDto;
import app.bys.bys_api.model.dto.bancamiga.BancamigaWebhookPayload;
import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.enums.PaymentStatus;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.repository.PaymentRepository;
import app.bys.bys_api.service.NotificationService;
import app.bys.bys_api.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Punto unico de matching/auto-aceptacion contra la API de conciliacion de Pago Movil de Bancamiga.
 * Nunca debe bloquear ni fallar el flujo normal de creacion de un pago: cualquier error de Bancamiga
 * deja el Payment en PENDING (para revision manual o el siguiente intento del sweep job).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BancamigaVerificationService {

    private static final double AMOUNT_TOLERANCE = 0.01;
    private static final DateTimeFormatter BANCAMIGA_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final PaymentRepository paymentRepository;
    private final BancamigaClient bancamigaClient;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Value("${bancamiga.enabled}")
    private boolean bancamigaEnabled;

    @Value("${bancamiga.receiving-phone}")
    private String receivingPhone;

    @Async
    public void verifyAndMaybeAcceptAsync(Long paymentId) {
        verifyAndMaybeAccept(paymentId);
    }

    public void verifyAndMaybeAccept(Long paymentId) {
        if (!bancamigaEnabled) {
            log.debug("Bancamiga deshabilitado (bancamiga.enabled=false), se omite la verificacion del pago {}", paymentId);
            return;
        }

        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.getPaymentStatus() != PaymentStatus.PENDING || payment.getPaymentType() != PaymentType.MOBILE) {
            return;
        }

        String bankCode = payment.getBank() != null ? payment.getBank().getBancamigaCode() : null;
        LocalDate date = payment.getPaymentDate() != null ? payment.getPaymentDate().toLocalDate() : null;
        String phone = buildFullPhone(payment);

        if (bankCode == null || date == null || phone == null) {
            log.debug("Payment {} no tiene datos suficientes para consultar Bancamiga (banco/fecha/telefono)", paymentId);
            return;
        }

        try {
            BancamigaFindPaymentResponse response = bancamigaClient.findPaymentMobile(phone, bankCode, date);
            if (response == null || response.getLista() == null) {
                return;
            }
            response.getLista().stream()
                    .filter(movement -> matches(payment, movement))
                    .findFirst()
                    .ifPresent(movement -> acceptFromMovement(paymentId, movement, "SYNC_CHECK"));
        } catch (RuntimeException e) {
            log.warn("No se pudo verificar el pago {} contra Bancamiga, queda PENDING para revision manual/sweep: {}",
                    paymentId, e.getMessage());
        }
    }

    public void handleWebhookMovement(BancamigaWebhookPayload webhookPayload) {
        if (!bancamigaEnabled) {
            log.debug("Bancamiga deshabilitado, se ignora el webhook con Refpk {}", webhookPayload.getRefpk());
            return;
        }

        BancamigaMovementDto movement = toMovementDto(webhookPayload);

        List<Payment> candidates = paymentRepository.findByPaymentTypeAndPaymentStatusAndPaymentDateAfter(
                PaymentType.MOBILE, PaymentStatus.PENDING, LocalDateTime.now().minusDays(30));

        candidates.stream()
                .filter(payment -> matches(payment, movement))
                .findFirst()
                .ifPresentOrElse(
                        payment -> acceptFromMovement(payment.getId(), movement, "WEBHOOK"),
                        () -> log.info("Webhook de Bancamiga con Refpk {} no matcheo ningun pago PENDING", movement.getRefpk())
                );
    }

    private void acceptFromMovement(Long paymentId, BancamigaMovementDto movement, String matchSource) {
        Payment payment = paymentRepository.findById(paymentId).orElse(null);
        if (payment == null || payment.getPaymentStatus() != PaymentStatus.PENDING) {
            return;
        }

        try {
            Payment accepted = paymentService.acceptPayment(paymentId);
            accepted.setBancamigaRefpk(movement.getRefpk());
            accepted.setBancamigaVerifiedAt(LocalDateTime.now());
            accepted.setBancamigaMatchSource(matchSource);
            paymentRepository.save(accepted);

            Long offerId = accepted.getOffer().getId();
            Long requestId = accepted.getOffer().getServiceRequest().getId();
            Long userId = accepted.getFinalUser().getId();
            Long providerId = accepted.getServiceProvider().getId();
            notificationService.notifyPaymentAccepted(userId, providerId, requestId, offerId);

            log.info("Pago {} auto-aceptado por Bancamiga (Refpk={}, fuente={})", paymentId, movement.getRefpk(), matchSource);
        } catch (ServiceRequestAlreadyAcceptedException e) {
            log.debug("Pago {} ya habia sido aceptado (carrera entre sweep/webhook): {}", paymentId, e.getMessage());
        }
    }

    private boolean matches(Payment payment, BancamigaMovementDto movement) {
        if (movement.getAmount() == null || payment.getAmountInBolivars() == null) {
            return false;
        }
        if (Math.abs(movement.getAmount() - payment.getAmountInBolivars()) > AMOUNT_TOLERANCE) {
            return false;
        }

        String expectedPhone = buildFullPhone(payment);
        if (expectedPhone == null || !expectedPhone.equals(movement.getPhoneOrig())) {
            return false;
        }

        if (payment.getBank() == null || payment.getBank().getBancamigaCode() == null
                || !payment.getBank().getBancamigaCode().equalsIgnoreCase(movement.getBancoOrig())) {
            return false;
        }

        LocalDate paymentDay = payment.getPaymentDate() != null ? payment.getPaymentDate().toLocalDate() : null;
        LocalDate movementDay = parseMovementDate(movement.getFechaMovimiento());
        if (paymentDay == null || !paymentDay.equals(movementDay)) {
            return false;
        }

        if (receivingPhone != null && !receivingPhone.isBlank()
                && movement.getPhoneDest() != null && !receivingPhone.equals(movement.getPhoneDest())) {
            log.debug("PhoneDest {} del movimiento no coincide con el telefono receptor configurado {} (no bloqueante)",
                    movement.getPhoneDest(), receivingPhone);
        }
        if (payment.getReferenceNumber() != null && movement.getNroReferenciaCorto() != null
                && !payment.getReferenceNumber().equals(movement.getNroReferenciaCorto())) {
            log.debug("Referencia declarada {} no coincide con NroReferenciaCorto {} de Bancamiga para el pago {} (no bloqueante)",
                    payment.getReferenceNumber(), movement.getNroReferenciaCorto(), payment.getId());
        }

        return true;
    }

    private LocalDate parseMovementDate(String fechaMovimiento) {
        if (fechaMovimiento == null) {
            return null;
        }
        try {
            return LocalDate.parse(fechaMovimiento, BANCAMIGA_DATE_FORMAT);
        } catch (Exception e) {
            log.warn("No se pudo parsear FechaMovimiento '{}' de Bancamiga", fechaMovimiento);
            return null;
        }
    }

    // Bancamiga espera/devuelve el teléfono como "58" + los 3 digitos del codigo (sin el 0 inicial) + el número,
    // ej. phoneCode=0414, phoneNumber=0000000 -> "584140000000".
    private String buildFullPhone(Payment payment) {
        if (payment.getPhoneCode() == null || payment.getPhoneNumber() == null) {
            return null;
        }
        String code = payment.getPhoneCode().getCode();
        return "58" + code.substring(1) + payment.getPhoneNumber();
    }

    private BancamigaMovementDto toMovementDto(BancamigaWebhookPayload payload) {
        BancamigaMovementDto movement = new BancamigaMovementDto();
        movement.setBancoOrig(payload.getBancoOrig());
        movement.setFechaMovimiento(payload.getFechaMovimiento());
        movement.setHoraMovimiento(payload.getHoraMovimiento());
        movement.setNroReferencia(payload.getNroReferencia());
        movement.setPhoneOrig(payload.getPhoneOrig());
        movement.setPhoneDest(payload.getPhoneDest());
        movement.setStatus(payload.getStatus());
        movement.setDescripcion(payload.getDescripcion());
        movement.setAmount(payload.getAmount() != null ? Double.valueOf(payload.getAmount()) : null);
        movement.setRefpk(payload.getRefpk());
        return movement;
    }
}
