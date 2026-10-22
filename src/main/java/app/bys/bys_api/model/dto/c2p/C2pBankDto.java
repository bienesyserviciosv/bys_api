package app.bys.bys_api.model.dto.c2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class C2pBankDto {

    @JsonProperty("code")
    private String code;

    @JsonProperty("name")
    private String name;
}
