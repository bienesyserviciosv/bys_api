package app.bys.bys_api.service;

import app.bys.bys_api.model.dto.GlobalStatsDto;
import app.bys.bys_api.repository.OfferRepository;
import app.bys.bys_api.repository.PaymentRepository;
import app.bys.bys_api.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GlobalStatsService {

    private final OfferRepository offerRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRequestRepository serviceRequestRepository;

    public GlobalStatsDto getGlobalStats() {
        return GlobalStatsDto.builder()
                .totalCompletedTransactions(paymentRepository.countCompletedTransactions())
                .totalOffers(offerRepository.countAllOffers())
                .totalRequests(serviceRequestRepository.countAllRequests())
                .averageAcceptanceDurationInHours(getAverageDuration())
                .build();
    }

    private long getAverageDuration() {
        Double avg = serviceRequestRepository.findAverageAcceptanceDurationInHours();
        return avg == null ? 0 : avg.longValue();
    }
}
