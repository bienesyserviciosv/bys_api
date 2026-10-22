package app.bys.bys_api.model.dto.c2p;

import app.bys.bys_api.model.enums.C2pAttemptStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class C2pAttemptAdminDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("status")
    private C2pAttemptStatus status;

    @JsonProperty("offer_id")
    private Long offerId;

    @JsonProperty("request_id")
    private Long requestId;

    @JsonProperty("user_id")
    private Long userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("payment_id")
    private Long paymentId;

    @JsonProperty("bank_code")
    private String bankCode;

    @JsonProperty("phone")
    private String phone;

    @JsonProperty("id_document")
    private String idDocument;

    @JsonProperty("amount_usd")
    private BigDecimal amountUsd;

    @JsonProperty("exchange_rate")
    private BigDecimal exchangeRate;

    @JsonProperty("amount_bs")
    private BigDecimal amountBs;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("error_code")
    private String errorCode;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    private LocalDateTime updatedAt;
}
