package app.bys.bys_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class GlobalStatsDto {

    @JsonProperty("total_requests")
    private long totalRequests;

    @JsonProperty("total_offers")
    private long totalOffers;

    @JsonProperty("total_completed_transactions")
    private long totalCompletedTransactions;

    @JsonProperty("total_profit")
    private Double totalProfit;

    @JsonProperty("average_acceptance_duration_in_hours")
    private long averageAcceptanceDurationInHours;
}
