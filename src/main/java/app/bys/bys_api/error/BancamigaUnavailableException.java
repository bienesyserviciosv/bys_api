package app.bys.bys_api.error;

public class BancamigaUnavailableException extends RuntimeException {
    public BancamigaUnavailableException(String message) {
        super(message);
    }

    public BancamigaUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
