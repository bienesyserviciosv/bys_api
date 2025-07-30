package app.bys.bys_api.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorMessage {

    //Validations
    public static final String EM_EMPTY_FIELD = "Shouldn't be empty";
    public static final String EM_ENTITY_NOT_FOUND = "Entity was not found";
    public static final String EM_WRONG_EMAIL = "The email address is incorrect";
}
