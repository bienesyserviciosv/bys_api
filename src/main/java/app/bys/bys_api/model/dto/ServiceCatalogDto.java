package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ServiceCatalogDto {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 3, max = 50, message = "The length must be between 3 and 50 char", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("name")
    private String name;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 10, max = 200, message = "The length must be between 10 and 200 char", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("description")
    private String description;

    @JsonProperty("specialization")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private SpecializationDto specialization;

    @JsonProperty("servicePictures")
    private Set<String> servicePictures = new HashSet<>();

    //  Constructor especial para JPQL

    public ServiceCatalogDto(
            Long id, String name,
            String description,
            Long specializationId,
            String specializationType
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.specialization = new SpecializationDto(specializationId, specializationType);
        this.servicePictures = new HashSet<>();
    }


}
