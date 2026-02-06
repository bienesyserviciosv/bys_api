package app.bys.bys_api.error;

public class InvalidStarRatingException extends RuntimeException {
    public InvalidStarRatingException() {
        super("The start rating is neither an integer nor a half-integer.");
    }
}
