package app.bys.bys_api.model.dto.bancamiga;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BancamigaFindPaymentResponse {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("lista")
    private List<BancamigaMovementDto> lista;

    @JsonProperty("mod")
    private String mod;

    @JsonProperty("num")
    private Integer num;
}
