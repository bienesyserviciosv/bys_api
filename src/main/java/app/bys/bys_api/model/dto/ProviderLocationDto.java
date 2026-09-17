package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ProviderLocationDto {

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @JsonProperty("latitude")
    private String latitude;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @JsonProperty("longitude")
    private String longitude;

}
