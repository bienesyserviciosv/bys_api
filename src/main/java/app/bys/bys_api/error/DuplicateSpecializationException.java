package app.bys.bys_api.error;

public class DuplicateSpecializationException extends RuntimeException {
    public DuplicateSpecializationException(String message) {
        super(message);
    }
}
