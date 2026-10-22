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
public class BancamigaC2pPaymentData {

    @JsonProperty("Amount")
    private Double amount;

    @JsonProperty("Banco")
    private String banco;

    @JsonProperty("Canal")
    private String canal;

    // "200" si fue aprobado, o un codigo de error (ej. "E029")
    @JsonProperty("Code")
    private String code;

    @JsonProperty("Created_at")
    private String createdAt;

    @JsonProperty("Dataorig")
    private String dataorig;

    @JsonProperty("Dni")
    private String dni;

    @JsonProperty("Error")
    private String error;

    @JsonProperty("ID")
    private String id;

    @JsonProperty("Nroref")
    private String nroref;

    @JsonProperty("Phonedest")
    private String phonedest;

    @JsonProperty("Phoneorig")
    private String phoneorig;

    // "APROBADO" o "NO APROBADO"
    @JsonProperty("Status")
    private String status;

    @JsonProperty("Update_at")
    private String updateAt;
}
