package app.bys.bys_api.error;

/**
 * La peticion llego a enviarse a Bancamiga pero no hubo respuesta a tiempo: el resultado es desconocido
 * (el cobro pudo haberse aprobado). Debe resolverse por conciliacion, nunca reintentando el cobro.
 */
public class BancamigaTimeoutException extends RuntimeException {
    public BancamigaTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
