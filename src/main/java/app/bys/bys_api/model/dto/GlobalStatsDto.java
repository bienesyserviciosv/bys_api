package app.bys.bys_api.model.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class GlobalStatsDto {

    private long totalRequests;
    private long totalOffers;
    private long totalCompletedTransactions;
}
