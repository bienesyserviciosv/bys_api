package app.bys.bys_api.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FcmTokenDto {

    @NotNull
    private String token;

    @NotNull
    private Long userId;
}
