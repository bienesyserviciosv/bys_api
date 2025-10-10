package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.model.enums.RequestStatus;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ServiceRequestMetricsDto {

//    @JsonProperty("id")
    private Long id;

//    @JsonProperty("client_name")
    private String clientName;

    private String email;

    private String phone;

//    @JsonProperty("description")
    private String description;

//    @JsonProperty("date")
    private LocalDate date;

//    @JsonProperty("time")
    private LocalTime time;

//    @JsonProperty("status")
    private RequestStatus requestStatus;

//    @JsonProperty("creation_date")
    private LocalDateTime creationDate;

//    @JsonProperty("acceptance_date")
    private LocalDateTime acceptanceDate;

//    @JsonProperty("offer_quantity")
    private Integer offerQuantity;

//    @JsonProperty("payment_amount")
    private Double paymentAmount;

//    @JsonProperty("payment_type")
    private PaymentType paymentType;

//    @JsonProperty("payment_date")
    private LocalDateTime paymentDate;


}
