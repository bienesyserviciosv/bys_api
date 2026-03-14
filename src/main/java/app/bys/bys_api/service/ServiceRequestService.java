package app.bys.bys_api.service;

import app.bys.bys_api.error.EmailNotVerifiedException;
import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.error.ConflictException;
import app.bys.bys_api.mapper.FinalUserMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.ServiceRequestMapper;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.ServiceRequestDto;
import app.bys.bys_api.model.dto.ServiceRequestMetricsDto;
import app.bys.bys_api.model.dto.ServiceRequestSummary;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Picture;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.enums.OfferStatus;
import app.bys.bys_api.model.enums.PictureType;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.RequestStatus;
import app.bys.bys_api.model.enums.UserStatus;
import app.bys.bys_api.repository.*;
import app.bys.bys_api.service.specification.ServiceRequestSpecification;
import app.bys.bys_api.utils.MediaConstants;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceRequestService {

    private final ServiceProviderRepository serviceProviderRepository;

    @Value("${media.url}")
    public String mediaUrl;

    private final ServiceRequestRepository serviceRequestRepository;
    private final FinalUserRepository finalUserRepository;
    private final ServiceRequestMapper requestMapper;
    private final FinalUserMapper userMapper;
    private final MediaRepository mediaRepository;
    private final PictureRepository pictureRepository;
    private final NotificationService notificationService;
    private final OfferRepository offerRepository;

    public ServiceRequestSummary get(Long id) {
        ServiceRequestSummary serviceRequestSummary = serviceRequestRepository.findRequestById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service request with id: " + id + " not found"));

        Set<String> pictures = pictureRepository.findPictureUrlByServiceRequestId(id).stream()
                .map(url -> url.startsWith(mediaUrl) ? url : mediaUrl + url)
                .collect(Collectors.toSet());

        serviceRequestSummary.setPictureSet(pictures);
        return serviceRequestSummary;

    }

    public PageDto<ServiceRequestSummary> getAll(Pageable pageable, String search, List<Long> specializationList, String address, List<Long> userList, List<Long> providerIdList, RequestStatus status, Authentication auth) throws BadRequestException {
        List<RequestStatus> allowedStatusForProviders = null;
        LocalDate today = LocalDate.now();
        boolean applyDateFilter = false;
        LocalDate sevenDaysLater = today.plusDays(7);
        LocalDateTime oneDayAgo = LocalDateTime.now().minusDays(1);

        if (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER"))) {
            allowedStatusForProviders = List.of(RequestStatus.CREATED);
            applyDateFilter = true;
            status = null;
        }
        Province province = null;
        if (address != null) {
            try {
                province = Province.valueOf(address);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid province: " + address);
            }
        }
        if (search == null) search = "";
        if (providerIdList != null && providerIdList.isEmpty()) providerIdList = null;

        Page<ServiceRequestSummary> page = serviceRequestRepository.findAllRequestSummariesFiltered(
                search, specializationList, province, userList, providerIdList, status,
                allowedStatusForProviders, applyDateFilter, today, sevenDaysLater, oneDayAgo, pageable
        );

        List<ServiceRequestSummary> enrichedList = page.getContent().stream()
                .peek(dto -> {
                    Set<String> rawPictures = pictureRepository.findPictureUrlByServiceRequestId(dto.getId());
                    Set<String> normalizedPictures = rawPictures.stream()
                            .filter(Objects::nonNull)
                            .map(url -> url.startsWith(mediaUrl) ? url : mediaUrl + url)
                            .collect(Collectors.toSet());
                    dto.setPictureSet(normalizedPictures);
                })
                .toList();

        Page<ServiceRequestSummary> enrichedPage = new PageImpl<>(enrichedList, pageable, page.getTotalElements());

        return PageMapper.pageToDto(enrichedPage);
    }

    private List<Specification<ServiceRequest>> getSpecificationsList(String search, List<Long> specializationList, String address, List<Long> userList, List<Long> providerList) {
        Specification<ServiceRequest> specializationSpec =
                specializationList != null ? ServiceRequestSpecification.hasSpecialization(specializationList)
                        : null;

        ServiceRequestSpecification searchSpec =
                search != null ? new ServiceRequestSpecification(
                        new SearchCriteria(
                                "description",
                                "s",
                                search
                        )
                )
                        : null;

        ServiceRequestSpecification addressSpec =
                address != null ? new ServiceRequestSpecification(
                        new SearchCriteria(
                                "address",
                                ":",
                                address
                        )
                )
                        : null;

        Specification<ServiceRequest> userSpec =
                userList != null ? ServiceRequestSpecification.hasUser(userList)
                        : null;

        Specification<ServiceRequest> providerSpec =
                providerList != null ? ServiceRequestSpecification.hasProvider(providerList)
                        : null;

        return new ArrayList<>(Arrays.asList(
                specializationSpec,
                searchSpec,
                addressSpec,
                userSpec,
                providerSpec
        ));
    }

    public PageDto<ServiceRequestMetricsDto> getAllRequestMetrics(Pageable pageable, String search, List<Long> specializationList, String address, List<Long> userList) throws BadRequestException {

        Province province = null;
        if (address != null) {
            try {
                province = Province.valueOf(address);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid province: " + address);
            }
        }
        if (search == null || search.isBlank()) search = "";
        if (specializationList != null && specializationList.isEmpty()) specializationList = null;
        if (userList != null && userList.isEmpty()) userList = null;

        // Se tradujeron los nombres de campos del DTO a propiedades reales de la entidad para que el sort funcione
        Pageable translatedPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), translateSort(pageable.getSort()));

        Page<ServiceRequestMetricsDto> page = serviceRequestRepository.findAllRequestMetrics(search, specializationList, province, userList, translatedPageable);

        return PageMapper.pageToDto(page);

    }

    private Sort translateSort(Sort sort) {
        return Sort.by(
                sort.stream()
                        .map(order -> {
                            if ("clientName".equals(order.getProperty())) {
                                return new Sort.Order(order.getDirection(), "finalUser.name");
                            }
                            return order;
                        })
                        .collect(Collectors.toList())
        );
    }


    public ServiceRequestMetricsDto getRequestMetrics(Long id) {
        return serviceRequestRepository.findRequestMetricsById(id)
                .orElseThrow(() -> new EntityNotFoundException("Service request with id: " + id + " not found"));
    }

    public ServiceRequest createWithUserId(Long userId, ServiceRequestDto serviceRequestDto, MultipartFile[] files) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

        return saveServiceRequest(serviceRequestDto, files, finalUser);
    }

    private ServiceRequest saveServiceRequest(ServiceRequestDto serviceRequestDto, MultipartFile[] files, FinalUser finalUser) {
        if (serviceRequestRepository.existsActiveRequestOfType(finalUser.getId(), serviceRequestDto.getSpecialization().getId())) {
            throw new ConflictException("You already have an active request for this specialization");
        }

        serviceRequestDto.setRequestStatus(RequestStatus.CREATED);
        serviceRequestDto.setOfferQuantity(0);
        serviceRequestDto.setNewOffer(false);
        serviceRequestDto.setFinalUser(userMapper.entityToDto(finalUser));
        serviceRequestDto.setCreationDate(LocalDateTime.now());
        ServiceRequest serviceRequest = serviceRequestRepository.save(requestMapper.dtoToEntity(serviceRequestDto));

        if (serviceRequest.getPictureSet() == null) {
            throw new IllegalStateException("pictureSet no fue inicializado");
        }
        uploadPictureSet(files, serviceRequest);

        //finalUser.setPendingRequests(finalUser.getPendingRequests() + 1);
        finalUser.setTotalRequests(finalUser.getTotalRequests() + 1);
        finalUserRepository.save(finalUser);

        // Notify providers of new request
        Long specializationId = serviceRequestDto.getSpecialization().getId();
        notificationService.notifyProvidersOfNewRequest(specializationId, serviceRequest);

        return serviceRequest;
    }

    public ServiceRequest create(String email, ServiceRequestDto serviceRequestDto, MultipartFile[] files) {
        FinalUser finalUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final user with email: " + email + " not found"));

        if (finalUser.getStatus().equals(UserStatus.INACTIVE)) {
            throw new EmailNotVerifiedException(ErrorMessage.EM_EMAIL_NOT_VERIFIED);
        }
        return saveServiceRequest(serviceRequestDto, files, finalUser);
    }

    public ServiceRequestDto update(Long id, ServiceRequestDto serviceRequestDto) {
        ServiceRequest requestFound = serviceRequestRepository.findById(id).orElseThrow();
        requestMapper.updateServiceRequestFromDto(serviceRequestDto, requestFound);
        return requestMapper.entityToDto(serviceRequestRepository.save(requestFound));
    }

    public void delete(Long id) {
        if (!serviceRequestRepository.existsById(id)) {
            throw new EntityNotFoundException("Service request with id: " + id + "not found");
        }
        serviceRequestRepository.deleteById(id);
    }

    private void uploadPictureSet(MultipartFile[] files, ServiceRequest serviceRequest) {
        if (files != null) {
            Arrays.stream(files).forEach(file -> {
                Picture picture = new Picture();
                picture.setServiceRequest(serviceRequest);
                picture.setUrl(uploadImage(file));
                picture.setPictureType(PictureType.SERVICE_REQUEST);
                serviceRequest.getPictureSet().add(picture);
                pictureRepository.save(picture);
            });
            serviceRequestRepository.save(serviceRequest);
        }
    }

    public String uploadImage(MultipartFile image) {
        if (image != null) {
            String imageName = MediaConstants.REQUEST_FOLDER + UUID.randomUUID();
            try {
                mediaRepository.saveImage(imageName, image);
                return imageName;
            } catch (IOException exception) {
                throw new RuntimeException("Error happened uploading the images: " + exception.getMessage());
            }
        }
        return null;
    }

    @Transactional
    public void markAsCompleted(String email, Long requestId) {
        FinalUser finalUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() ->  new EntityNotFoundException("Final User with email: " + email + " not found"));

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service request with id: " + requestId + " not found"));

        if (!serviceRequest.getFinalUser().getId().equals(finalUser.getId())) {
            throw new ConflictException("The service request with id: " + requestId + " does not belong to this user");
        }

        if (serviceRequest.getRequestStatus() != RequestStatus.ACCEPTED) {
            throw new ConflictException("The service request with id: " + requestId + " hasn't been accepted");
        }

        finalUser.setCompletedRequests(finalUser.getCompletedRequests() + 1);
        finalUserRepository.save(finalUser);

        serviceRequest.setRequestStatus(RequestStatus.COMPLETED);
        Offer acceptedOffer = serviceRequest.getAcceptedOffer();
        if (acceptedOffer != null) {
            acceptedOffer.setStatus(OfferStatus.COMPLETED);
            offerRepository.save(acceptedOffer);
        }
        ServiceProvider serviceProvider = serviceRequest.getServiceProvider();
        if (serviceProvider == null) {
            throw new RuntimeException("Service Request without a Service Provider associated");
        }
        serviceProvider.setCompletedServices(serviceProvider.getCompletedServices() + 1);
        serviceProviderRepository.save(serviceProvider);

        serviceRequestRepository.save(serviceRequest);
    }
}
