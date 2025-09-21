package app.bys.bys_api.error;

public class ServiceRequestAlreadyAcceptedException extends RuntimeException {
    public ServiceRequestAlreadyAcceptedException(String message) {
        super(message);
    }
}
