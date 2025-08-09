package app.bys.bys_api.service;

import app.bys.bys_api.mapper.OfferMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.OfferRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.repository.ServiceRequestRepository;
import app.bys.bys_api.service.specification.OfferSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepo;
    private final OfferMapper offerMapper;
    private final ServiceProviderRepository serviceProviderRepo;
    private final ServiceRequestRepository serviceRequestRepo;
    private final FinalUserRepository finalUserRepo;

    public OfferDto get(Long id) {
        return offerMapper.entityToDto(offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found")));
    }

    public PageDto<OfferDto> getAll(Pageable pageable, String search, List<Long> providerIdList) {

        Specification<Offer> providerSpec =
                providerIdList != null ? OfferSpecification.hasProvider(providerIdList)
                        : null;

        OfferSpecification searchSpec =
                search != null ? new OfferSpecification(
                        new SearchCriteria(
                                "name",
                                "s",
                                search
                        )
                )
                        : null;

        List<Specification<Offer>> specList = new ArrayList<>(Arrays.asList(
                providerSpec,
                searchSpec
        ));

        return PageMapper.pageToDto(offerRepo.findAll(
                Specification.allOf(specList.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())),
                pageable).map(offerMapper::entityToDto));
    }

    public OfferDto create(String email, OfferDto offerDto) {
        ServiceProvider provider = serviceProviderRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Provider with email: " + email + " not found"));

        Offer offer = offerMapper.dtoToEntity(offerDto);
        offer.setProvider(provider);

        return offerMapper.entityToDto(offerRepo.save(offer));
    }

    public OfferDto update(Long id, OfferDto offerDto) {
        Offer storedOffer = offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found"));

        offerMapper.updateOfferFromDto(offerDto, storedOffer);
        return offerMapper.entityToDto(offerRepo.save(storedOffer));
    }

    public void delete(Long id) {
        if (!offerRepo.existsById(id)) {
            throw new EntityNotFoundException("Offer with id: " + id + "not found");
        }
        offerRepo.deleteById(id);
    }

    public OfferDto acceptOffer(String email, Long id) {
        FinalUser finalUser = finalUserRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User with email: " + email + " not found"));

        Offer offer = offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found"));

        offer.setFinalUser(finalUser);
        offer.setAccepted(true);

       /* MOVER ESTA LOGICA A CUANDO SE ACEPTA EL PAGO
        ServiceRequest serviceRequest = serviceRequestRepo.findById(offer.getServiceRequestId())
                .orElseThrow(() -> new EntityNotFoundException("Request Service with id: " + offer.getServiceRequestId() + " not found"));

        serviceRequest.setRequestStatus(RequestStatus.ACCEPTED);
        serviceRequestRepo.save(serviceRequest);*/

        return offerMapper.entityToDto(offerRepo.save(offer));
    }
}
