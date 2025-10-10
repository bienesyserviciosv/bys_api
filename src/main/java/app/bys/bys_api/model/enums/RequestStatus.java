package app.bys.bys_api.model.enums;

public enum RequestStatus {
    CREATED,//Solicitud creada
    IN_PROGRESS,//Solicitud con oferta aceptada
    PENDING,//Solicitud con pago creado NOTIFICACION AL ADMINISTRADOR
    ACCEPTED,//Solicitud con pago aceptado
    IN_REVIEW,//Solicitud en revisión cuando se rechazó un pago NOTIFICACION AL CLIENTE EXPLICANDO PORQUE NO FUE ACEPTADO EL PAGO
    COMPLETED,
    CANCELLED
}
