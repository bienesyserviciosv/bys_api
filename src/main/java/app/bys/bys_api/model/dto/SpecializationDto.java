package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SpecializationDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("specializationType")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = {OnCreate.class, OnUpdate.class})
    private String specializationType;

}
