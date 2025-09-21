package app.bys.bys_api.config;

import app.bys.bys_api.error.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleEntityNotFound(EntityNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler({DuplicateEmailException.class, DuplicatePhoneException.class})
    public ResponseEntity<Map<String, Object>> handleDuplicateFields(RuntimeException ex) {
        String field = ex instanceof DuplicateEmailException ? "email" : "phone";

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of(
                        "message", ex.getMessage(),
                        "error", "DUPLICATE_ENTRY",
                        "field", field,
                        "timestamp", LocalDateTime.now()
                ));
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<Map<String, Object>> handleEmailNotVerified(EmailNotVerifiedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message", ex.getMessage(),
                        "error", "EMAIL_NOT_VERIFIED",
                        "timestamp", LocalDateTime.now()
                ));
    }

    @ExceptionHandler(InvalidPictureException.class)
    public ResponseEntity<Map<String, String>> handleBadRequest(InvalidPictureException ex) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", "INVALID_PICTURE");
        errorBody.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
    }

    @ExceptionHandler(ServiceRequestAlreadyAcceptedException.class)
    public ResponseEntity<Map<String, String>> handleRequestAlreadyAccepted(ServiceRequestAlreadyAcceptedException ex) {
        Map<String, String> errorBody = new HashMap<>();
        errorBody.put("error", "REQUEST_ALREADY_ACCEPTED");
        errorBody.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody);
    }

    @ExceptionHandler(ForbiddenActionException.class)
    public ResponseEntity<Map<String, Object>> handleForbiddenAction(ForbiddenActionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of(
                        "message", ex.getMessage(),
                        "error", "FORBIDDEN_ACTION",
                        "timestamp", LocalDateTime.now()
                ));
    }

}
