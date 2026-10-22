package app.bys.bys_api.model.dto.c2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Monto a cobrar por una oferta con la tasa vigente. La app lo muestra antes de pedir la OTP. */
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class C2pQuoteDto {

    @JsonProperty("offer_id")
    private Long offerId;

    @JsonProperty("amount_usd")
    private BigDecimal amountUsd;

    @JsonProperty("exchange_rate")
    private BigDecimal exchangeRate;

    @JsonProperty("exchange_rate_date")
    private LocalDateTime exchangeRateDate;

    @JsonProperty("amount_bs")
    private BigDecimal amountBs;

    @JsonProperty("max_attempts")
    private int maxAttempts;

    @JsonProperty("attempts_remaining")
    private int attemptsRemaining;

    // false si el pago en linea no esta disponible para esta oferta (ver unavailable_reason)
    @JsonProperty("available")
    private boolean available;

    @JsonProperty("unavailable_reason")
    private String unavailableReason;
}
