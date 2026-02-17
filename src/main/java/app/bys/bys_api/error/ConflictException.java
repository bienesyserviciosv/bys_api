package app.bys.bys_api.error;

public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}

