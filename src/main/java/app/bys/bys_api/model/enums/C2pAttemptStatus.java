package app.bys.bys_api.model.enums;

/**
 * Estado de un intento de cobro C2P.
 * <ul>
 *   <li>PROCESSING: se esta enviando el cobro a Bancamiga.</li>
 *   <li>APPROVED: Bancamiga aprobo el cobro (el dinero se debito al cliente).</li>
 *   <li>REJECTED: Bancamiga respondio NO APROBADO (OTP vencido, fondos insuficientes, etc.). Cuenta para el limite de intentos.</li>
 *   <li>ERROR: el cobro no llego a procesarse (Bancamiga no disponible, error de formato, token). No cuenta para el limite.</li>
 *   <li>UNKNOWN: se envio el cobro pero no hubo respuesta (timeout). Se resuelve con la conciliacion (c2p/mov).</li>
 *   <li>REVIEW: no se pudo resolver automaticamente; requiere revision manual de un administrador.</li>
 * </ul>
 */
public enum C2pAttemptStatus {
    PROCESSING,
    APPROVED,
    REJECTED,
    ERROR,
    UNKNOWN,
    REVIEW
}
