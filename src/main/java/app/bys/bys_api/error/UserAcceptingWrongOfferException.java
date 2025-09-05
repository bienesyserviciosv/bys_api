package app.bys.bys_api.error;

public class UserAcceptingWrongOfferException extends RuntimeException {
    public UserAcceptingWrongOfferException(String message) {
        super(message);
    }
}
