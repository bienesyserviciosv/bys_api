package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OfferDto {

    @JsonProperty("id")
    private Long id;

    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Positive(message = "Must be greater than 0", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("price")
    private Double price;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 3, max = 30, message = "The length must be between 3 and 30", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("duration")
    private String duration;

    @Size(min = 3, max = 100, message = "The length must be between 3 and 100", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("description")
    private String description;

    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Positive(message = "Must be greater than 0", groups = OnCreate.class)
    @JsonProperty("service_request_id")
    private Long serviceRequestId;

    @JsonProperty(value = "provider_id", access = JsonProperty.Access.READ_ONLY)
    private Long providerId;

    @JsonProperty("accepted")
    private Boolean accepted = false;

}
