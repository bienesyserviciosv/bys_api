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
public class BancamigaErrorBody {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("mensaje")
    private String mensaje;

    @JsonProperty("mod")
    private String mod;

    // Presente en algunas respuestas de error (ej. "Session Expirada"), en vez de "code"/"mensaje".
    @JsonProperty("status")
    private Integer status;

    @JsonProperty("title")
    private String title;
}
