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
public class BancamigaC2pBanksResponse {

    @JsonProperty("code")
    private Integer code;

    @JsonProperty("data")
    private List<BancamigaC2pBank> data;

    @JsonProperty("mod")
    private String mod;
}
