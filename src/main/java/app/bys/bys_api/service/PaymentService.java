package app.bys.bys_api.service;

import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.mapper.PaymentMapper;
import app.bys.bys_api.model.dto.MobilePaymentDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.TransferPaymentDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.PaymentRepository;
import app.bys.bys_api.repository.PictureRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PictureRepository pictureRepository;
    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;

    public Object getPayment(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Payment with id: " + id + " not found"));
        return switch (payment.getPaymentType()) {
            case MOBILE -> paymentMapper.entityToMobileDto(payment);
            case TRANSFER -> paymentMapper.entityToTransferDto(payment);
        };
    }

    public PageDto<Object> getAllPayments(Pageable pageable) {
        Page<Payment> payments = paymentRepository.findAll(pageable);

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


    public MobilePaymentDto createMobilePayment(MobilePaymentDto mobilePaymentDto) {
        FinalUser finalUser = finalUserRepository.findById(mobilePaymentDto.getFinalUserId())
                .orElseThrow(() -> new EntityNotFoundException("User with id: " + mobilePaymentDto.getFinalUserId() + " not found"));
        ServiceProvider serviceProvider = serviceProviderRepository.findById(mobilePaymentDto.getServiceProviderId())
                .orElseThrow(()-> new EntityNotFoundException("Service Provider with id: " + mobilePaymentDto.getServiceProviderId() + " not found"));

        Payment mobilePayment = paymentMapper.mobileDtoToEntity(mobilePaymentDto);

        mobilePayment.setFinalUser(finalUser);
        finalUser.getPaymentSet().add(mobilePayment);

        mobilePayment.setServiceProvider(serviceProvider);
        serviceProvider.getPaymentSet().add(mobilePayment);

        mobilePayment.setPaymentType(PaymentType.MOBILE);

        return paymentMapper.entityToMobileDto(paymentRepository.save(mobilePayment));
    }

     public TransferPaymentDto createTransferPayment(TransferPaymentDto transferPaymentDto) {
         FinalUser finalUser = finalUserRepository.findById(transferPaymentDto.getFinalUserId())
                 .orElseThrow(() -> new EntityNotFoundException("User with id: " + transferPaymentDto.getFinalUserId() + " not found"));
         ServiceProvider serviceProvider = serviceProviderRepository.findById(transferPaymentDto.getServiceProviderId())
                 .orElseThrow(()-> new EntityNotFoundException("Service Provider with id: " + transferPaymentDto.getServiceProviderId() + " not found"));

         Payment transferPayment = paymentMapper.transferDtoToEntity(transferPaymentDto);

         transferPayment.setFinalUser(finalUser);
         finalUser.getPaymentSet().add(transferPayment);

         transferPayment.setServiceProvider(serviceProvider);
         serviceProvider.getPaymentSet().add(transferPayment);

        transferPayment.setPaymentType(PaymentType.TRANSFER);
        return paymentMapper.entityToTransferDto(paymentRepository.save(transferPayment));
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
}
