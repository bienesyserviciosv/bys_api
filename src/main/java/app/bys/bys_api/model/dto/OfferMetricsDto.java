package app.bys.bys_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OfferMetricsDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("worker_name")
    private String workerName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("price")
    private Double price;

    @JsonProperty("service_request_id")
    private Long serviceRequestId;

    @JsonProperty("created_at")
    private LocalDateTime createdAt;

    @JsonProperty("accepted")
    private Boolean accepted;

    @JsonProperty("accepted_at")
    private LocalDateTime acceptedAt;
}
