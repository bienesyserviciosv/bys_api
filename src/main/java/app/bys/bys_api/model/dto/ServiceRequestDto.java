package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.Status;
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
    @NotBlank
    private String description;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("address")
    @NotBlank
    private String address;

    @JsonProperty("date")
    private Date date;

    @JsonProperty("time")
    private Time time;

    @JsonProperty("status")
    @Enumerated(EnumType.STRING)
    private Status status = Status.IN_PROGRESS;

    @JsonProperty("specialization")
    @NotNull(groups = OnCreate.class)
    private SpecializationDto specialization;

    @JsonProperty("finalUser")
    private FinalUserDto finalUser;

//    private ServiceProviderDto serviceProvider;


}
