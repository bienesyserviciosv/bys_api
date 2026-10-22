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
public class BancamigaC2pMovement {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("dnidest")
    private String dnidest;

    @JsonProperty("phonedest")
    private String phonedest;

    @JsonProperty("phoneorig")
    private String phoneorig;

    @JsonProperty("amount")
    private Double amount;

    @JsonProperty("dni")
    private String dni;

    @JsonProperty("banco")
    private String banco;

    @JsonProperty("nroref")
    private String nroref;

    @JsonProperty("dataorig")
    private String dataorig;

    @JsonProperty("code")
    private String code;

    @JsonProperty("error")
    private String error;

    @JsonProperty("status")
    private String status;

    @JsonProperty("canal")
    private String canal;

    @JsonProperty("motivo")
    private String motivo;
}
