package app.bys.bys_api.error;

public class BancamigaTokenExpiredException extends RuntimeException {
    public BancamigaTokenExpiredException(String message) {
        super(message);
    }
}
