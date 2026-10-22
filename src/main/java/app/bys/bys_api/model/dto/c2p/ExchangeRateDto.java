package app.bys.bys_api.model.dto.c2p;

import app.bys.bys_api.model.enums.ExchangeRateSource;
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
public class ExchangeRateDto {

    @JsonProperty("rate")
    private BigDecimal rate;

    @JsonProperty("source")
    private ExchangeRateSource source;

    @JsonProperty("fetched_at")
    private LocalDateTime fetchedAt;

    @JsonProperty("created_by")
    private String createdBy;
}
