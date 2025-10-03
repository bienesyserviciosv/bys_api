package app.bys.bys_api.service;

import app.bys.bys_api.error.ForbiddenActionException;
import app.bys.bys_api.mapper.OfferMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.enums.RequestStatus;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.OfferRepository;
import app.bys.bys_api.repository.ServiceRequestRepository;
import app.bys.bys_api.service.specification.OfferSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ServiceRequestRepository serviceRequestRepo;
    private final FinalUserRepository finalUserRepo;

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

        if (!serviceRequest.getRequestStatus().equals(RequestStatus.CREATED) && !serviceRequest.getRequestStatus().equals(RequestStatus.IN_PROGRESS)) {
            throw new ForbiddenActionException("Cannot create offers for requests with payments created");
        }

        Integer offerQuantity = serviceRequest.getOfferQuantity();
        offerQuantity++;

        serviceRequest.setOfferQuantity(offerQuantity);
        serviceRequest.setNewOffer(true);

        serviceRequestRepo.save(serviceRequest);

        Offer offer = offerMapper.dtoToEntity(offerDto);
        offer.setProvider(provider);
        offer.setServiceRequest(serviceRequest);

        offer.setCreatedAt(LocalDateTime.now());

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

    @Transactional
    public OfferDto acceptOffer(String email, Long id) {
        FinalUser finalUser = finalUserRepo.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User with email: " + email + " not found"));

        Offer offer = offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found"));

        ServiceRequest request = serviceRequestRepo.findById(offer.getServiceRequest().getId())
                .orElseThrow(() -> new EntityNotFoundException("Request Service with id: " + offer.getServiceRequest().getId() + " not found in this offer"));

        //Validación de estado de la solicitud. Si tiene el estado valido es que puede aceptar una oferta
        if (!request.getRequestStatus().equals(RequestStatus.CREATED) && !request.getRequestStatus().equals(RequestStatus.IN_PROGRESS)) {
            throw new ForbiddenActionException("Cannot accept offers of requests with payments created");
        }

        if (!offer.getServiceRequest().getFinalUser().equals(finalUser)) {
            throw new ForbiddenActionException("The user can only accept offers from requests they made");
        }

        if (offer.equals(request.getAcceptedOffer())) {
            throw new ForbiddenActionException("This offer is already the accepted one");
        }

        if (!request.getOfferSet().contains(offer)) {
            throw new IllegalArgumentException("This offer does not belong to the service request");
        }

        Offer previousAccepted = request.getAcceptedOffer();
        if (previousAccepted != null) {
            previousAccepted.setAccepted(false);
            previousAccepted.setAcceptedAt(null);
            offerRepo.save(previousAccepted);
        }

        offer.setFinalUser(finalUser);
        offer.setAccepted(true);
        offer.setAcceptedAt(LocalDateTime.now());

        request.setAcceptedOffer(offer);
        request.setRequestStatus(RequestStatus.IN_PROGRESS);
        serviceRequestRepo.save(request);

        return offerMapper.entityToDto(offerRepo.save(offer));
    }
}
