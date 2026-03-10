package app.bys.bys_api.model.enums;

import lombok.Getter;

@Getter
public enum NotificationType {
    NEW_REQUEST("Nueva solicitud disponible"),
    PAID_OFFER("Su oferta a la solicitud fue aceptada"),
    PAYMENT_ACCEPTED("Su pago fue aceptado"),
    NEW_OFFER("Su solicitud tiene una nueva oferta");

    private final String message;

    NotificationType(String message) {
        this.message = message;
    }

}
