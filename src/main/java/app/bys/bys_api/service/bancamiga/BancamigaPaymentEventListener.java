package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.model.event.MobilePaymentCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Desacopla la creación de un pago MOBILE (service.PaymentService) de la verificación contra Bancamiga:
 * evita que PaymentService dependa directamente de BancamigaVerificationService (que a su vez depende
 * de PaymentService para reutilizar acceptPayment), y mantiene el flujo normal de pago ajeno a Bancamiga.
 */
@Component
@RequiredArgsConstructor
public class BancamigaPaymentEventListener {

    private final BancamigaVerificationService bancamigaVerificationService;

    @EventListener
    public void onMobilePaymentCreated(MobilePaymentCreatedEvent event) {
        bancamigaVerificationService.verifyAndMaybeAcceptAsync(event.paymentId());
    }
}
