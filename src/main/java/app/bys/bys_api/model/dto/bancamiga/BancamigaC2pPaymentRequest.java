package app.bys.bys_api.model.dto.bancamiga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Body de POST /public/protected/c2p/payments/v2. Nunca registrar este objeto en logs (contiene la OTP).
 */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BancamigaC2pPaymentRequest {

    @JsonProperty("Banco_origen")
    private String bancoOrigen;

    @JsonProperty("Telf_origen")
    private String telfOrigen;

    @JsonProperty("Dni_origen")
    private String dniOrigen;

    // String con maximo 2 decimales y coma como separador decimal, ej. "10,50"
    @JsonProperty("Monto")
    private String monto;

    @JsonProperty("Motivo")
    private String motivo;

    @JsonProperty("Otp")
    private String otp;

    @Override
    public String toString() {
        return "BancamigaC2pPaymentRequest[banco=" + bancoOrigen + ", monto=" + monto + ", motivo=" + motivo + "]";
    }
}
