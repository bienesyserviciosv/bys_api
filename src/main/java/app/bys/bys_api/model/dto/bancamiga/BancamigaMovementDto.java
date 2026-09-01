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
public class BancamigaMovementDto {

    @JsonProperty("ID")
    private String id;

    @JsonProperty("Dni")
    private String dni;

    @JsonProperty("PhoneDest")
    private String phoneDest;

    @JsonProperty("PhoneOrig")
    private String phoneOrig;

    @JsonProperty("Amount")
    private Double amount;

    @JsonProperty("BancoOrig")
    private String bancoOrig;

    @JsonProperty("NroReferenciaCorto")
    private String nroReferenciaCorto;

    @JsonProperty("NroReferencia")
    private String nroReferencia;

    @JsonProperty("HoraMovimiento")
    private String horaMovimiento;

    @JsonProperty("FechaMovimiento")
    private String fechaMovimiento;

    @JsonProperty("Descripcion")
    private String descripcion;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Refpk")
    private String refpk;

    @JsonProperty("Ref")
    private Long ref;
}
