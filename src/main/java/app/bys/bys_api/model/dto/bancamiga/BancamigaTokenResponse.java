package app.bys.bys_api.model.dto.bancamiga;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BancamigaTokenResponse {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("expireDate")
    private Long expireDate;

    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("mod")
    private String mod;

    // Bancamiga responde el campo con este nombre (con el typo tal cual lo documentan).
    @JsonProperty("refresToken")
    private String refreshToken;

    @JsonProperty("token")
    private String token;
}
