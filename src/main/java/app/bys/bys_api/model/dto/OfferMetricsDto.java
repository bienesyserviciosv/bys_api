package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.OfferStatus;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OfferMetricsDto {

    private Long id;
    private String workerName;
    private String description;
    private Double price;
    private Long serviceRequestId;
    private LocalDateTime createdAt;
    private OfferStatus status;
    private LocalDateTime acceptedAt;
}
