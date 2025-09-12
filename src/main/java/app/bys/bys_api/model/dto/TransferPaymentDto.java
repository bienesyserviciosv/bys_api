package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.enums.BankName;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.validation.OnCreate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class TransferPaymentDto {

    @JsonProperty("id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @JsonProperty("bank")
    private BankName bank;

    @JsonProperty("screenshot")
    private String screenshot;

    @JsonProperty("id_number")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private String idNumber;

    @JsonProperty("reference_number")
    @Size(min = 6, max = 6, groups = OnCreate.class, message = "Must have 6 numbers")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private String referenceNumber;

    @JsonProperty ("payment_date")
    private LocalDateTime paymentDate;

    @JsonProperty("final_user_id")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private Long finalUserId;

    @JsonProperty("service_provider_id")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private Long serviceProviderId;

    @JsonProperty("offer_id")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private Long offerId;

    @Enumerated(EnumType.STRING)
    @JsonProperty ("payment_type")
    private PaymentType paymentType;
}
