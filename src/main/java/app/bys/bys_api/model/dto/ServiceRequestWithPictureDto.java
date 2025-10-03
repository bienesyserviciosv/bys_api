package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.RequestStatus;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ServiceRequestWithPictureDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("description")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 5, groups = {OnCreate.class, OnUpdate.class}, message = "The length must be greater than 5 char")
    private String description;

    @JsonProperty("address")
    @Enumerated(EnumType.STRING)
    private Province address;

    @Future(groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("time")
    private LocalTime time;

    @JsonProperty("status")
    private RequestStatus requestStatus;

    @JsonProperty(value = "creation_date", access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime creationDate;

    @JsonProperty("specialization")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private SpecializationDto specialization;

    @JsonProperty("final_user")
    private FinalUserDto finalUser;

    @Builder.Default
    @JsonProperty("offer_quantity")
    @PositiveOrZero(message = "Must be positive", groups = {OnCreate.class, OnUpdate.class})
    private Integer offerQuantity = 0;

    @JsonProperty("picture_set")
    private Set<String> pictureSet = new HashSet<>();

    @Builder.Default
    @JsonProperty("new_offer")
    private Boolean newOffer = false;

    @JsonProperty("service_provider_id")
    private Long serviceProviderId;

}
