package app.bys.bys_api.model.dto.bancamiga;

import app.bys.bys_api.error.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BancamigaCredentialSeedDto {

    @JsonProperty("token")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    private String token;

    @JsonProperty("refresh_token")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    private String refreshToken;
}
