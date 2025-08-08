package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.enums.RequestStatus;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ServiceRequestDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("description")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    private String description;

    @JsonProperty("address")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    private String address;

    @Future(groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("time")
    private LocalTime time;

    @Builder.Default
    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus = RequestStatus.PENDING;

    @JsonProperty("specialization")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private SpecializationDto specialization;

    @JsonProperty("finalUser")
    private FinalUserDto finalUser;

}
