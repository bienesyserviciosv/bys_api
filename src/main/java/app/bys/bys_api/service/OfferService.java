package app.bys.bys_api.service;

import app.bys.bys_api.error.UserAcceptingWrongOfferException;
import app.bys.bys_api.mapper.OfferMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
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

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepo;
    private final OfferMapper offerMapper;
    private final ServiceRequestRepository serviceRequestRepo;
    private final FinalUserRepository finalUserRepo;
    private final ServiceProviderRepository serviceProviderRepo;

    public OfferDto get(Long id) {
        return offerMapper.entityToDto(offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found")));
    }

    public PageDto<OfferDto> getAll(Pageable pageable, String search, List<Long> providerIdList, Long serviceRequestId) {

        Specification<Offer> providerSpec =
                providerIdList != null ? OfferSpecification.hasProvider(providerIdList)
                        : null;

        Specification<Offer> serviceRequestSpec =
                serviceRequestId != null ? OfferSpecification.hasServiceRequestId(serviceRequestId)
                        : null;


        OfferSpecification searchSpec =
                search != null ? new OfferSpecification(
                        new SearchCriteria(
                                "description",
                                "s",
                                search
                        )
                )
                        : null;

        List<Specification<Offer>> specList = new ArrayList<>(Arrays.asList(
                providerSpec,
                serviceRequestSpec,
                searchSpec
        ));

        return PageMapper.pageToDto(offerRepo.findAll(
                Specification.allOf(specList.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())),
                pageable).map(offerMapper::entityToDto));
    }

    public OfferDto create(ServiceProvider provider, OfferDto offerDto) {
        Long requestId = offerDto.getServiceRequestId();
        ServiceRequest serviceRequest = serviceRequestRepo.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " + requestId + " not found"));

        Integer offerQuantity = serviceRequest.getOfferQuantity();
        offerQuantity++;

        serviceRequest.setOfferQuantity(offerQuantity);
        serviceRequest.setNewOffer(true);

        serviceRequestRepo.save(serviceRequest);

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

        if (!isUserAuthorizedToAcceptThisOffer(finalUser, offer)) {
            throw new UserAcceptingWrongOfferException("The user can only accept offers from requests they made");
        }

        offer.setFinalUser(finalUser);
        offer.setAccepted(true);

        ServiceProvider serviceProvider = offer.getProvider();
        Set<ServiceRequest> servRequestSet = serviceProvider.getServiceRequestSet();
        ServiceRequest request = serviceRequestRepo.findById(offer.getServiceRequestId())
                .orElseThrow(() -> new EntityNotFoundException("Request Service with id: " + offer.getServiceRequestId() + " not found in this offer"));
        servRequestSet.add(request);
        request.setServiceProvider(serviceProvider);
        serviceProviderRepo.save(serviceProvider);
        serviceRequestRepo.save(request);

        return offerMapper.entityToDto(offerRepo.save(offer));
    }

    private boolean isUserAuthorizedToAcceptThisOffer(FinalUser finalUser, Offer offer) {
        Long requestId = offer.getServiceRequestId();

        return finalUser.getServiceRequest().stream()
                .map(ServiceRequest::getId)
                .anyMatch(id -> id.equals(requestId));
    }
}
