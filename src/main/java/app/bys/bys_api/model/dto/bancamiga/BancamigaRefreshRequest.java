package app.bys.bys_api.model.dto.bancamiga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class BancamigaRefreshRequest {

    @JsonProperty("refresh_token")
    private String refreshToken;
}
