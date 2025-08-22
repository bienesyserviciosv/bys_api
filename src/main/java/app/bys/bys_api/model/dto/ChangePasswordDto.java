package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChangePasswordDto {

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Size(min = 8, message = "The length must be greater than 8 char")
    @JsonProperty("current_password")
    private String currentPassword;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Size(min = 8, message = "The length must be greater than 8 char")
    @JsonProperty("new_password")
    private String newPassword;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Size(min = 8, message = "The length must be greater than 8 char")
    @JsonProperty("new_password_repeated")
    private String newPasswordRepeated;
}
