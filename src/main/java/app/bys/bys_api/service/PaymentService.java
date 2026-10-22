package app.bys.bys_api.service;

import app.bys.bys_api.error.ConflictException;
import app.bys.bys_api.error.ServiceRequestAlreadyAcceptedException;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.PaymentMapper;
import app.bys.bys_api.model.dto.MobilePaymentDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.TransferPaymentDto;
import app.bys.bys_api.model.entity.*;
import app.bys.bys_api.model.enums.*;
import app.bys.bys_api.model.event.MobilePaymentCreatedEvent;
import app.bys.bys_api.repository.*;
import app.bys.bys_api.utils.MediaConstants;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PictureRepository pictureRepository;
    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final OfferRepository offerRepository;
    private final MediaRepository mediaRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final NotificationService notificationService;
    private final ApplicationEventPublisher eventPublisher;
    private final C2pAttemptRepository c2pAttemptRepository;

    // Un pago C2P en curso o aprobado impide registrar un pago manual para la misma oferta
    private static final Set<C2pAttemptStatus> ACTIVE_C2P_STATUSES = Set.of(
            C2pAttemptStatus.PROCESSING, C2pAttemptStatus.UNKNOWN, C2pAttemptStatus.APPROVED, C2pAttemptStatus.REVIEW);

    @Value("${media.url}")
    public String mediaUrl;

    public Object getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));
        return switch (payment.getPaymentType()) {
            case MOBILE, C2P -> paymentMapper.entityToMobileDto(payment);
            case TRANSFER -> paymentMapper.entityToTransferDto(payment);
        };
    }

    public PageDto<Object> getAllPayments(Pageable pageable, String search, List<Long> userIdList, List<Long> providerIdList, PaymentStatus paymentStatus) throws BadRequestException {

        BankName bankFilter = null;
        if (search != null && !search.isBlank()) {
            try {
                bankFilter = BankName.fromDisplayName(search);
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid bank: " + search);
            }
        }

        Page<TransferPaymentDto> transfers = paymentRepository.findTransferPayments(bankFilter, userIdList, providerIdList, paymentStatus, pageable);
        Page<MobilePaymentDto> mobiles = paymentRepository.findMobilePayments(bankFilter, userIdList, providerIdList, paymentStatus, pageable);
        Page<MobilePaymentDto> c2pPayments = paymentRepository.findC2pPayments(bankFilter, userIdList, providerIdList, paymentStatus, pageable);

        List<Object> combined = Stream.concat(
                        Stream.concat(
                                transfers.getContent().stream().map(dto -> (Object) dto),
                                mobiles.getContent().stream().map(dto -> (Object) dto)),
                        c2pPayments.getContent().stream().map(dto -> (Object) dto)
                ).peek(dto -> {
            // Los pagos C2P no tienen comprobante (screenshot null)
            if (dto instanceof TransferPaymentDto transfer && transfer.getScreenshot() != null) {
                transfer.setScreenshot(mediaUrl + transfer.getScreenshot());
            } else if (dto instanceof MobilePaymentDto mobile && mobile.getScreenshot() != null) {
                mobile.setScreenshot(mediaUrl + mobile.getScreenshot());
            }
        }).toList();

        Page<Object> resultPage = new PageImpl<>(combined, pageable,
                transfers.getTotalElements() + mobiles.getTotalElements() + c2pPayments.getTotalElements());
        return PageMapper.pageToDto(resultPage);
    }

    public MobilePaymentDto createMobilePayment(MobilePaymentDto mobilePaymentDto, MultipartFile picture, String authenticatedEmail) {
        Offer offer = offerRepository.findById(mobilePaymentDto.getOfferId())
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + mobilePaymentDto.getOfferId() + " not found"));

        ServiceProvider serviceProvider = offer.getProvider();
        ServiceRequest request = offer.getServiceRequest();
        FinalUser finalUser = request.getFinalUser();

        if (!authenticatedEmail.equals(finalUser.getEmail())) {
            throw new ConflictException("You cannot create a payment for another user's request");
        }

        if (offer.getPayment() != null) {
            throw new ConflictException("Offer with id: " + offer.getId() + " already has a payment set");
        }

        if (!offer.getId().equals(request.getAcceptedOffer().getId())) {
            throw new ConflictException("The offer has not been accepted");
        }

        checkNoActiveC2p(offer);

        Payment mobilePayment = paymentMapper.mobileDtoToEntity(mobilePaymentDto);

        mobilePayment.setFinalUser(finalUser);
        mobilePayment.setServiceProvider(serviceProvider);
        mobilePayment.setOffer(offer);
        mobilePayment.setPaymentType(PaymentType.MOBILE);
        mobilePayment.setPaymentStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(mobilePayment);
        attachScreenshotToPayment(picture, savedPayment);

        request.setRequestStatus(RequestStatus.PENDING);
        serviceRequestRepository.save(request);
        notificationService.notifyAdminOfNewPayment(
                finalUser.getId(),
                serviceProvider.getId(),
                request.getId(),
                mobilePayment.getPaymentType()
        );
        eventPublisher.publishEvent(new MobilePaymentCreatedEvent(savedPayment.getId()));
        return paymentMapper.entityToMobileDto(savedPayment);
    }

    public TransferPaymentDto createTransferPayment(TransferPaymentDto transferPaymentDto, MultipartFile picture, String authenticatedEmail) {
        Offer offer = offerRepository.findById(transferPaymentDto.getOfferId())
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + transferPaymentDto.getOfferId() + " not found"));

        ServiceProvider serviceProvider = offer.getProvider();
        ServiceRequest request = offer.getServiceRequest();
        FinalUser finalUser = request.getFinalUser();

        if (!authenticatedEmail.equals(finalUser.getEmail())) {
            throw new ConflictException("You cannot create a payment for another user's request");
        }

        if (offer.getPayment() != null) {
            throw new ConflictException("Offer with id: " + offer.getId() + " already has a payment set");
        }

        if (!offer.getId().equals(request.getAcceptedOffer().getId())) {
            throw new ConflictException("The offer has not been accepted");
        }

        checkNoActiveC2p(offer);

        Payment transferPayment = paymentMapper.transferDtoToEntity(transferPaymentDto);

        transferPayment.setFinalUser(finalUser);
        transferPayment.setServiceProvider(serviceProvider);
        transferPayment.setOffer(offer);
        transferPayment.setPaymentType(PaymentType.TRANSFER);
        transferPayment.setPaymentStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(transferPayment);
        attachScreenshotToPayment(picture, savedPayment);

        request.setRequestStatus(RequestStatus.PENDING);
        serviceRequestRepository.save(request);
        notificationService.notifyAdminOfNewPayment(finalUser.getId(),
                serviceProvider.getId(),
                request.getId(),
                transferPayment.getPaymentType()
        );
        return paymentMapper.entityToTransferDto(savedPayment);
    }

    public MobilePaymentDto updateMobilePayment(Long id, MobilePaymentDto mobilePaymentDto) {
        Payment mobilePaymentFound = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));
        paymentMapper.updatePaymentFromMobileDto(mobilePaymentDto, mobilePaymentFound);
        return paymentMapper.entityToMobileDto(paymentRepository.save(mobilePaymentFound));
    }

    public TransferPaymentDto updateTransferPayment(Long id, TransferPaymentDto transferPaymentDto) {
        Payment transferPaymentFound = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));
        paymentMapper.updatePaymentFromTransferDto(transferPaymentDto, transferPaymentFound);
        return paymentMapper.entityToTransferDto(paymentRepository.save(transferPaymentFound));
    }

    @Transactional
    public void delete(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));

        if (payment.getPaymentType() == PaymentType.C2P) {
            throw new ConflictException("Los pagos en linea (C2P) ya fueron cobrados por el banco y no se pueden eliminar");
        }

        try {
            pictureRepository.findByPaymentId(id).ifPresent(picture -> {
                try {
                    mediaRepository.deleteImage(picture.getUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete image from S3: {}", picture.getUrl(), e);
                }
                pictureRepository.delete(picture);
            });
            Offer offer = payment.getOffer();
            offer.setPayment(null);
            offerRepository.save(offer);

            ServiceRequest serviceRequest = offer.getServiceRequest();
            serviceRequest.setRequestStatus(RequestStatus.IN_PROGRESS);
            if (payment.getPaymentStatus() == PaymentStatus.ACCEPTED) {
                serviceRequest.setAcceptanceDate(null);
                serviceRequest.setServiceProvider(null);
            }
            serviceRequestRepository.save(serviceRequest);

            FinalUser finalUser = payment.getFinalUser();
            finalUser.getPaymentSet().remove(payment);
            finalUserRepository.save(finalUser);

            ServiceProvider serviceProvider = payment.getServiceProvider();
            serviceProvider.getPaymentSet().remove(payment);
            serviceProviderRepository.save(serviceProvider);

            paymentRepository.delete(payment);

        } catch (Exception e) {
            log.error("Error deleting payment with id {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete payment", e);
        }
    }

    @Transactional
    public Payment acceptPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));

        Long requestId = payment.getOffer().getServiceRequest().getId();
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " + requestId + " not found"));

//        FinalUser finalUser = payment.getFinalUser();
//        finalUser.setAcceptedRequests(finalUser.getAcceptedRequests() + 1);
//        finalUserRepository.save(finalUser);

        if (serviceRequest.getRequestStatus() == RequestStatus.ACCEPTED) {
            throw new ServiceRequestAlreadyAcceptedException("Service Request with id: " + requestId + " already accepted");
        }

        if (serviceRequest.getAcceptedOffer() == null) {
            throw new ConflictException("Service Request with id: " + requestId + " doesn't have an accepted offer");
        }

        payment.setPaymentStatus(PaymentStatus.ACCEPTED);
        paymentRepository.save(payment);

        serviceRequest.setServiceProvider(serviceRequest.getAcceptedOffer().getProvider());
        serviceRequest.setRequestStatus(RequestStatus.ACCEPTED);
        serviceRequest.setAcceptanceDate(LocalDateTime.now());
        serviceRequestRepository.save(serviceRequest);

        return payment;
    }

    @Transactional
    public Payment rejectPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));

        Long requestId = payment.getOffer().getServiceRequest().getId();
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " + requestId + " not found"));

        if (serviceRequest.getAcceptedOffer() == null) {
            throw new ConflictException("Service Request with id: " + requestId + " doesn't have an accepted offer");
        }

        if (payment.getPaymentType() == PaymentType.C2P) {
            throw new ConflictException("Los pagos en linea (C2P) ya fueron cobrados por el banco y no se pueden rechazar");
        }

        payment.setPaymentStatus(PaymentStatus.REJECTED);
        paymentRepository.save(payment);

        serviceRequest.setRequestStatus(RequestStatus.IN_REVIEW);
        serviceRequestRepository.save(serviceRequest);

        return payment;

    }

    private void checkNoActiveC2p(Offer offer) {
        if (c2pAttemptRepository.existsByOfferIdAndStatusIn(offer.getId(), ACTIVE_C2P_STATUSES)) {
            throw new ConflictException("Esta oferta tiene un pago en linea en proceso o aprobado");
        }
    }

    public void attachScreenshotToPayment(MultipartFile screenshot, Payment payment) {
        if (screenshot != null) {
            Picture picture = new Picture();
            picture.setPayment(payment);
            String url = uploadImage(screenshot);
            picture.setUrl(url);
            picture.setPictureType(PictureType.PAYMENT_PROOF);
            payment.setScreenshot(url);
            pictureRepository.save(picture);
        }
    }

    public String uploadImage(MultipartFile image) {
        if (image != null) {
            String imageName = MediaConstants.PAYMENT_FOLDER + UUID.randomUUID();
            try {
                mediaRepository.saveImage(imageName, image);
                return imageName;
            } catch (IOException exception) {
                throw new RuntimeException("Error happened uploading the images: " + exception.getMessage());
            }
        }
        return null;
    }
}
