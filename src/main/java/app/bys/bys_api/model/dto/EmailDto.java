package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EmailDto {

    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD)
    @Email(message = ErrorMessage.EM_WRONG_EMAIL)
    @Size(max = 50)
    @JsonProperty("email")
    private String email;

}
