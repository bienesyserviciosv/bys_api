package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthRequestDto {

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    private String identifier;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    private String password;
}



