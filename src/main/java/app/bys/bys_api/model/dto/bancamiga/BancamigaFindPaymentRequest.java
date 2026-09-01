package app.bys.bys_api.model.dto.bancamiga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BancamigaFindPaymentRequest {

    @JsonProperty("Phone")
    private String phone;

    @JsonProperty("Bank")
    private String bank;

    @JsonProperty("Date")
    private String date;
}
