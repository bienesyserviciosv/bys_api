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
public class BancamigaWebhookPayload {

    @JsonProperty("BancoOrig")
    private String bancoOrig;

    @JsonProperty("FechaMovimiento")
    private String fechaMovimiento;

    @JsonProperty("HoraMovimiento")
    private String horaMovimiento;

    @JsonProperty("NroReferencia")
    private String nroReferencia;

    @JsonProperty("PhoneOrig")
    private String phoneOrig;

    @JsonProperty("PhoneDest")
    private String phoneDest;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Descripcion")
    private String descripcion;

    @JsonProperty("Amount")
    private String amount;

    @JsonProperty("Refpk")
    private String refpk;
}
