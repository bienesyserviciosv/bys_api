package app.bys.bys_api.service;

import app.bys.bys_api.error.ForbiddenActionException;
import app.bys.bys_api.mapper.OfferMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.OfferDto;
import app.bys.bys_api.model.dto.OfferMetricsDto;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferService {

    private final OfferRepository offerRepo;
    private final OfferMapper offerMapper;
    private final ServiceRequestRepository serviceRequestRepo;
    private final FinalUserRepository finalUserRepo;
    private final OfferRepository offerRepository;

    public OfferDto get(Long id) {
        return offerMapper.entityToDto(offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found")));
    }

    public OfferMetricsDto getMetrics(Long id) {
        return offerMapper.entityToMetricsDto(offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found")));
    }

    public PageDto<OfferDto> getAll(Pageable pageable, String search, List<Long> providerIdList, List<Long> serviceRequestIdList, Boolean accepted, List<Long> userIdList) {

        if (providerIdList != null && providerIdList.isEmpty()) providerIdList = null;
        if (serviceRequestIdList != null && serviceRequestIdList.isEmpty()) serviceRequestIdList = null;
        if (userIdList != null && userIdList.isEmpty()) userIdList = null;
        if (search == null) search = "";

        Page<OfferDto> page = offerRepository.findAllOffersFiltered(search, serviceRequestIdList, providerIdList, accepted, userIdList, pageable);

        return PageMapper.pageToDto(page);
    }

    private List<Specification<Offer>> getSpecificationList(String search, List<Long> providerIdList, List<Long> serviceRequestIdList, Boolean accepted, List<Long> userIdList){
        Specification<Offer> providerSpec =
                providerIdList != null ? OfferSpecification.hasProvider(providerIdList)
                        : null;

        Specification<Offer> userSpec =
                userIdList != null ? OfferSpecification.hasUser(userIdList)
                        : null;

        Specification<Offer> serviceRequestSpec =
                serviceRequestIdList != null ? OfferSpecification.hasServiceRequest(serviceRequestIdList)
                        : null;

        Specification<Offer> acceptedSpec =
                accepted != null ? OfferSpecification.isAccepted(accepted)
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

        return new ArrayList<>(Arrays.asList(
                providerSpec,
                userSpec,
                serviceRequestSpec,
                searchSpec,
                acceptedSpec
        ));

    }

    public PageDto<OfferMetricsDto> getAllOfferMetrics(Pageable pageable, String search, List<Long> providerIdList, List<Long> serviceRequestIdList, Boolean accepted) {

        if (providerIdList != null && providerIdList.isEmpty()) providerIdList = null;
        if (serviceRequestIdList != null && serviceRequestIdList.isEmpty()) serviceRequestIdList = null;
        if (search == null) search = "";

        // Se tradujeron los nombres de campos del DTO a propiedades reales de la entidad para que el sort funcione
        Pageable translatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), translateSort(pageable.getSort()));

        Page<OfferMetricsDto> page = offerRepository.findAllOfferMetricsFiltered(search, serviceRequestIdList, providerIdList, accepted, translatedPageable);

        return PageMapper.pageToDto(page);
    }

    private Sort translateSort(Sort sort) {
        Map<String, String> sortMapping = Map.of(
                "workerName", "provider.name",
                "serviceRequestId", "serviceRequest.id"
        );
        return Sort.by(
                sort.stream()
                        .map(order -> {
                            String mappedProperty = sortMapping.getOrDefault(order.getProperty(), order.getProperty());
                            return new Sort.Order(order.getDirection(), mappedProperty);
                        })
                        .collect(Collectors.toList())
        );
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
        offer.setAccepted(false);
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
        Offer offer = offerRepo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + id + " not found"));
        Long requestId = offer.getServiceRequest().getId();
        ServiceRequest serviceRequest = serviceRequestRepo.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " + requestId + " not found"));
        Integer offerQuantity = serviceRequest.getOfferQuantity();
        offerQuantity--;
        serviceRequest.setOfferQuantity(offerQuantity);
        serviceRequestRepo.save(serviceRequest);

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
            previousAccepted.setFinalUser(null);
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
