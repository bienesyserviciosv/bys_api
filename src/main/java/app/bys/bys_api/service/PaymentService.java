package app.bys.bys_api.service;

import app.bys.bys_api.error.ForbiddenActionException;
import app.bys.bys_api.error.ServiceRequestAlreadyAcceptedException;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.PaymentMapper;
import app.bys.bys_api.model.dto.MobilePaymentDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.TransferPaymentDto;
import app.bys.bys_api.model.entity.*;
import app.bys.bys_api.model.enums.PaymentStatus;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.model.enums.PictureType;
import app.bys.bys_api.model.enums.RequestStatus;
import app.bys.bys_api.repository.*;
import app.bys.bys_api.service.specification.PaymentSpecification;
import app.bys.bys_api.utils.MediaConstants;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

    public Object getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));
        return switch (payment.getPaymentType()) {
            case MOBILE -> paymentMapper.entityToMobileDto(payment);
            case TRANSFER -> paymentMapper.entityToTransferDto(payment);
        };
    }

    public PageDto<Object> getAllPayments(Pageable pageable, String search, List<Long> userIdList, List<Long> providerIdList) {

        PaymentSpecification searchSpec =
                search != null ? new PaymentSpecification(
                        new SearchCriteria(
                                "bank",
                                "s",
                                search
                        )
                )
                        : null;

        Specification<Payment> userSpec =
                userIdList != null ? PaymentSpecification.hasUser(userIdList)
                        : null;

        Specification<Payment> providerSpec =
                providerIdList != null ? PaymentSpecification.hasProvider(providerIdList)
                        : null;


        List<Specification<Payment>> specList = new ArrayList<>(Arrays.asList(
                searchSpec,
                userSpec,
                providerSpec
        ));

        Page<Payment> payments = paymentRepository.findAll(
                Specification.allOf(specList.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())),
                pageable);

        List<Object> mappedPayments = payments.stream()
                .map(payment -> {
                    if (payment.getPaymentType() == PaymentType.MOBILE) {
                        return paymentMapper.entityToMobileDto(payment);
                    } else if (payment.getPaymentType() == PaymentType.TRANSFER) {
                        return paymentMapper.entityToTransferDto(payment);
                    } else {
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .toList();

        Page<Object> mappedPage = new PageImpl<>(mappedPayments, pageable, payments.getTotalElements());
        return PageMapper.pageToDto(mappedPage);
    }


    public MobilePaymentDto createMobilePayment(MobilePaymentDto mobilePaymentDto, MultipartFile picture) {
        FinalUser finalUser = finalUserRepository.findById(mobilePaymentDto.getFinalUserId())
                .orElseThrow(() -> new EntityNotFoundException("User with id: " + mobilePaymentDto.getFinalUserId() + " not found"));
        ServiceProvider serviceProvider = serviceProviderRepository.findById(mobilePaymentDto.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with id: " + mobilePaymentDto.getServiceProviderId() + " not found"));
        Offer offer = offerRepository.findById(mobilePaymentDto.getOfferId())
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + mobilePaymentDto.getOfferId() + " not found"));
        ServiceRequest request = serviceRequestRepository.findById(offer.getServiceRequest().getId())
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " +  offer.getServiceRequest().getId() + " not found"));

        if (offer.getPayment() != null){
            throw new ForbiddenActionException("Offer with id: " + offer.getId() + " already has a payment set");
        }

        if (!offer.equals(request.getAcceptedOffer())) {
            throw new ForbiddenActionException("The offer has not been accepted");
        }

        Payment mobilePayment = paymentMapper.mobileDtoToEntity(mobilePaymentDto);

        mobilePayment.setFinalUser(finalUser);
        finalUser.getPaymentSet().add(mobilePayment);

        mobilePayment.setServiceProvider(serviceProvider);
        serviceProvider.getPaymentSet().add(mobilePayment);

        mobilePayment.setOffer(offer);
        offer.setPayment(mobilePayment);

        mobilePayment.setPaymentType(PaymentType.MOBILE);
        mobilePayment.setPaymentStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(mobilePayment);
        attachScreenshotToPayment(picture, savedPayment);

        return paymentMapper.entityToMobileDto(savedPayment);
    }

    public TransferPaymentDto createTransferPayment(TransferPaymentDto transferPaymentDto, MultipartFile picture) {
        FinalUser finalUser = finalUserRepository.findById(transferPaymentDto.getFinalUserId())
                .orElseThrow(() -> new EntityNotFoundException("User with id: " + transferPaymentDto.getFinalUserId() + " not found"));
        ServiceProvider serviceProvider = serviceProviderRepository.findById(transferPaymentDto.getServiceProviderId())
                .orElseThrow(() -> new EntityNotFoundException("Service Provider with id: " + transferPaymentDto.getServiceProviderId() + " not found"));
        Offer offer = offerRepository.findById(transferPaymentDto.getOfferId())
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + transferPaymentDto.getOfferId() + " not found"));
        ServiceRequest request = serviceRequestRepository.findById(offer.getServiceRequest().getId())
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " +  offer.getServiceRequest().getId() + " not found"));

        if (offer.getPayment() != null){
            throw new ForbiddenActionException("Offer with id: " + offer.getId() + " already has a payment set");
        }

        if (!offer.equals(request.getAcceptedOffer())) {
            throw new ForbiddenActionException("The offer has not been accepted");
        }

        Payment transferPayment = paymentMapper.transferDtoToEntity(transferPaymentDto);

        transferPayment.setFinalUser(finalUser);
        finalUser.getPaymentSet().add(transferPayment);

        transferPayment.setServiceProvider(serviceProvider);
        serviceProvider.getPaymentSet().add(transferPayment);

        transferPayment.setOffer(offer);
        offer.setPayment(transferPayment);

        transferPayment.setPaymentType(PaymentType.TRANSFER);
        transferPayment.setPaymentStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(transferPayment);
        attachScreenshotToPayment(picture, savedPayment);
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

    public void delete(Long id) {
        if (!paymentRepository.existsById(id)) {
            throw new EntityNotFoundException("Payment with id: " + id + " not found");
        }

        pictureRepository.findByPaymentId(id).ifPresent(pictureRepository::delete);

        paymentRepository.deleteById(id);
    }

    @Transactional
    public Payment acceptPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));

        Long requestId = payment.getOffer().getServiceRequest().getId();
        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " + requestId + " not found"));

        if (serviceRequest.getRequestStatus()==RequestStatus.ACCEPTED) {
            throw new ServiceRequestAlreadyAcceptedException("Service Request with id: " + requestId + " already accepted");
        }

        if (serviceRequest.getAcceptedOffer() == null){
            throw new ForbiddenActionException("Service Request with id: " + requestId + " doesn't have an accepted offer");
        }

        payment.setPaymentStatus(PaymentStatus.ACCEPTED);
        paymentRepository.save(payment);

        serviceRequest.setServiceProvider(serviceRequest.getAcceptedOffer().getProvider());
        serviceRequest.setRequestStatus(RequestStatus.ACCEPTED);
        serviceRequestRepository.save(serviceRequest);

        return payment;
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
