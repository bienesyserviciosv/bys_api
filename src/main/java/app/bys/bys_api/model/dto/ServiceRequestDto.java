package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.enums.RequestStatus;
import app.bys.bys_api.validation.OnCreate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.sql.Time;
import java.util.Date;

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

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("address")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    private String address;

    @JsonProperty("date")
    private Date date;

    @JsonProperty("time")
    private Time time;

    @Builder.Default
    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus = RequestStatus.IN_PROGRESS;

    @JsonProperty("specialization")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private SpecializationDto specialization;

    @JsonProperty("finalUser")
    private FinalUserDto finalUser;

//    private ServiceProviderDto serviceProvider;


}
