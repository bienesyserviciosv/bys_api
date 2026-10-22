package app.bys.bys_api.model.dto.c2p;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.enums.PhoneCode;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class C2pPaymentRequestDto {

    @JsonProperty("offer_id")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD)
    private Long offerId;

    // Codigo del banco del pagador, de la lista GET /payment/c2p/banks (ej. "0102")
    @JsonProperty("bank_code")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Pattern(regexp = "\\d{4}", message = "Codigo de banco invalido")
    private String bankCode;

    @JsonProperty("phone_code")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD)
    private PhoneCode phoneCode;

    @JsonProperty("phone_number")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Pattern(regexp = "\\d{7}", message = "El numero debe tener 7 digitos")
    private String phoneNumber;

    // V (venezolano) o E (extranjero)
    @JsonProperty("id_type")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Pattern(regexp = "[VE]", message = "Tipo de documento invalido")
    private String idType;

    @JsonProperty("id_number")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Pattern(regexp = "\\d{5,9}", message = "Numero de cedula invalido")
    private String idNumber;

    // Clave de pago generada por el banco del pagador. Nunca se guarda ni se registra en logs.
    @JsonProperty("otp")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Pattern(regexp = "\\d{4,12}", message = "Clave de pago invalida")
    private String otp;

    // Monto en Bs que el cliente vio y confirmo (de /quote). Si la tasa cambio, se rechaza para que confirme el nuevo.
    @JsonProperty("expected_amount_bs")
    private BigDecimal expectedAmountBs;

    @Override
    public String toString() {
        return "C2pPaymentRequestDto[offerId=" + offerId + ", bankCode=" + bankCode + "]";
    }
}
