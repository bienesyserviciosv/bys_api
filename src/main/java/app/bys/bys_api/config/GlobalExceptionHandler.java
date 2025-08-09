package app.bys.bys_api.config;

import app.bys.bys_api.error.DuplicateEmailException;
import app.bys.bys_api.error.DuplicatePhoneException;
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
}
