package app.bys.bys_api.service;

import app.bys.bys_api.model.dto.GlobalStatsDto;
import app.bys.bys_api.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GlobalStatsService {

    private final OfferRepository offerRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;

    public GlobalStatsDto getGlobalStats() {
        return GlobalStatsDto.builder()
                .totalCompletedTransactions(paymentRepository.countCompletedTransactions())
                .totalOffers(offerRepository.countAllOffers())
                .totalClients(finalUserRepository.countAllFinalUser())
                .totalServiceProviders(serviceProviderRepository.countAllServiceProvider())
                .serviceProvidersNotVerified(serviceProviderRepository.countByAdminVerifiedFalse())
                .totalRequests(serviceRequestRepository.countAllRequests())
                .pendingPayments(paymentRepository.countPendingTransactions())
                .totalProfit(offerRepository.calculateTotalProfit())
                .averageAcceptanceDurationInHours(getAverageDuration())
                .build();
    }

    private long getAverageDuration() {
        Double avg = serviceRequestRepository.findAverageAcceptanceDurationInHours();
        return avg == null ? 0 : avg.longValue();
    }
}
