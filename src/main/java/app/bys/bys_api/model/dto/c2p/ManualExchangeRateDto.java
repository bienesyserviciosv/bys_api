package app.bys.bys_api.model.dto.c2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ManualExchangeRateDto {

    @JsonProperty("rate")
    @NotNull
    @DecimalMin(value = "0.0001")
    private BigDecimal rate;
}
