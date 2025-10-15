package app.bys.bys_api.model.enums;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

@Getter
public enum Province {
    BARCELONA("BARCELONA"),
    PUERTO_LA_CRUZ("PUERTO LA CRUZ"),
    LECHERIA("LECHERIA"),
    NUEVA_BARCELONA("NUEVA BARCELONA"),
    GUANTA("GUANTA");

    @JsonValue
    private final String displayName;

    Province(String displayName) {
        this.displayName = displayName;
    }
}
