package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.enums.PaymentStatus;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reintenta la verificacion de pagos MOBILE en PENDING que el chequeo sincrono (al crear el pago) no
 * logro confirmar todavia -- cubre condiciones de carrera con Bancamiga y caidas puntuales del servicio.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BancamigaPendingPaymentsScheduler {

    private final PaymentRepository paymentRepository;
    private final BancamigaVerificationService bancamigaVerificationService;

    @Value("${bancamiga.enabled}")
    private boolean bancamigaEnabled;

    @Value("${bancamiga.pending-sweep.lookback-days}")
    private int lookbackDays;

    @Scheduled(fixedDelayString = "${bancamiga.pending-sweep.fixed-delay-ms}")
    public void sweepPendingPayments() {
        if (!bancamigaEnabled) {
            log.debug("Bancamiga deshabilitado, se omite el barrido de pagos pendientes");
            return;
        }

        LocalDateTime cutoff = LocalDateTime.now().minusDays(lookbackDays);
        List<Payment> pending = paymentRepository.findByPaymentTypeAndPaymentStatusAndPaymentDateAfter(
                PaymentType.MOBILE, PaymentStatus.PENDING, cutoff);

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Barriendo {} pago(s) MOBILE pendiente(s) contra Bancamiga", pending.size());
        for (Payment payment : pending) {
            bancamigaVerificationService.verifyAndMaybeAccept(payment.getId());
        }
    }
}
