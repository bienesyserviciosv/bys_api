package app.bys.bys_api.model.enums;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public enum PhoneCode {
    CODE_0412("0412"),
    CODE_0414("0414"),
    CODE_0416("0416"),
    CODE_0422("0422"),
    CODE_0424("0424"),
    CODE_0426("0426"),;

    @JsonProperty
    private final String code;

    PhoneCode(String code) {
        this.code = code;
    }
}
