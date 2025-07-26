package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.SpecializationType;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
    @NotNull(groups = {OnCreate.class, OnUpdate.class})
    @Enumerated(EnumType.STRING)
    private SpecializationType specializationType;

}
