package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.RequestStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class ServiceRequestSummary {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("description")
    private String description;

    @JsonProperty("address")
    private Province address;

    @JsonProperty("date")
    private LocalDate date;

    @JsonProperty("time")
    private LocalTime time;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("status")
    private RequestStatus requestStatus;

    @JsonProperty(value = "creation_date", access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime creationDate;

    @JsonProperty(value = "acceptance_date", access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime acceptanceDate;

    @JsonProperty("specialization")
    private String specialization;

    @JsonProperty("final_user_id")
    private Long finalUserId;

    @JsonProperty("offer_quantity")
    private Integer offerQuantity;

    @JsonProperty("picture_set")
    private Set<String> pictureSet = new HashSet<>();

    @JsonProperty("new_offer")
    private Boolean newOffer;


    public ServiceRequestSummary(Long id, String description, Province address,
                                 LocalDate date, LocalTime time, String latitude, String longitude,
                                 RequestStatus requestStatus, LocalDateTime creationDate, LocalDateTime acceptanceDate,
                                 String specialization, Long finalUserId, Integer offerQuantity,
                                 Boolean newOffer) {

        this.id = id;
        this.description = description;
        this.address = address;
        this.date = date;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
        this.requestStatus = requestStatus;
        this.creationDate = creationDate;
        this.acceptanceDate = acceptanceDate;
        this.specialization = specialization;
        this.finalUserId = finalUserId;
        this.offerQuantity = offerQuantity;
        this.newOffer = newOffer;
        this.pictureSet = new HashSet<>();
    }
}
