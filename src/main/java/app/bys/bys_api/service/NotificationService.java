package app.bys.bys_api.service;

import app.bys.bys_api.error.ForbiddenActionException;
import app.bys.bys_api.mapper.NotificationMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.PaymentNotificationDto;
import app.bys.bys_api.model.entity.*;
import app.bys.bys_api.model.enums.NotificationType;
import app.bys.bys_api.repository.*;
import app.bys.bys_api.service.specification.NotificationSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final NotificationMapper notificationMapper;
    private final FinalUserRepository finalUserRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final PaymentRepository paymentRepository;
    private final SimpMessagingTemplate messagingTemplate;

    public PageDto<NotificationDto> getAllNotifications(Pageable pageable, String search, List<Long> providerIdList, List<Long> finalUserIdList) {

        Specification<Notification> providerSpec =
                providerIdList != null ? NotificationSpecification.hasProvider(providerIdList)
                        : null;

        Specification<Notification> userSpec =
                finalUserIdList != null ? NotificationSpecification.hasUser(finalUserIdList)
                        : null;

        NotificationSpecification searchSpec =
                search != null ? new NotificationSpecification(
                        new SearchCriteria(
                                "name",
                                "s",
                                search
                        )
                )
                        : null;

        List<Specification<Notification>> specList = new ArrayList<>(Arrays.asList(
                providerSpec,
                userSpec,
                searchSpec
        ));

        return PageMapper.pageToDto(notificationRepository.findAll(
                Specification.allOf(specList.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())),
                pageable).map(notificationMapper::toDto));

    }

    public NotificationDto getById(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification with id " + notificationId + " not found"));

        return notificationMapper.toDto(notification);
    }

    public NotificationDto readNotification(Long notificationId, String email) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new EntityNotFoundException("Notification with id " + notificationId + " not found"));

        Optional<FinalUser> finalUserOpt = finalUserRepository.findByEmail(email);
        Optional<ServiceProvider> providerOpt = serviceProviderRepository.findByEmail(email);

        if (finalUserOpt.isPresent() && notification.getFinalUser() != null) {
            if (Objects.equals(notification.getFinalUser().getId(), finalUserOpt.get().getId())) {
                notification.setRead(true);
                notificationRepository.save(notification);
                return notificationMapper.toDto(notification);
            }
        }

        if (providerOpt.isPresent() && notification.getServiceProvider() != null) {
            if (Objects.equals(notification.getServiceProvider().getId(), providerOpt.get().getId())) {
                notification.setRead(true);
                notificationRepository.save(notification);
                return notificationMapper.toDto(notification);
            }
        }

        throw new ForbiddenActionException("The user can't read this notification");
    }

    public void notifyProvidersOfNewRequest(Long specializationId, ServiceRequest serviceRequest) {

        List<ServiceProvider> providers = serviceProviderRepository.findBySpecializations_Id(specializationId);

        if (providers != null && !providers.isEmpty()) {
            List<Notification> notifications = providers.stream()
                    .map(provider -> Notification.builder()
                            .serviceProvider(provider)
                            .read(false)
                            .timestamp(LocalDateTime.now())
                            .serviceRequest(serviceRequest)
                            .notificationType(NotificationType.NEW_REQUEST)
                            .build())
                    .collect(Collectors.toList());

            notificationRepository.saveAll(notifications);
            log.info("{} notification created", notifications.size());
        } else {
            log.warn("No providers found with this conditions");
        }
    }

    public void notifyPaymentAccepted(Long userId, Long providerId, Long requestId) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

        ServiceProvider serviceProvider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceProvider with id: " + providerId + " not found"));

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceRequest with id: " + requestId + " not found"));

        Notification providerNotification = Notification.builder()
                .serviceProvider(serviceProvider)
                .read(false)
                .serviceRequest(serviceRequest)
                .timestamp(LocalDateTime.now())
                .notificationType(NotificationType.PAID_OFFER)
                .build();


        Notification userNotification = Notification.builder()
                .finalUser(finalUser)
                .read(false)
                .serviceRequest(serviceRequest)
                .timestamp(LocalDateTime.now())
                .notificationType(NotificationType.PAYMENT_ACCEPTED)
                .build();

        notificationRepository.save(providerNotification);
        notificationRepository.save(userNotification);

    }

    public void notifyAdminsOfNewPayment(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found with ID: " + paymentId));

        List<FinalUser> admins = finalUserRepository.findAdmins();

        if (admins.isEmpty()) {
            log.warn("No admins found to notify about payment ID: {}", paymentId);
            return;
        }

        List<Notification> notifications = admins.stream()
                .map(admin -> Notification.builder()
                        .finalUser(admin)
                        .read(false)
                        .timestamp(LocalDateTime.now())
                        .payment(payment)
                        .build())
                .collect(Collectors.toList());

        notificationRepository.saveAll(notifications);

    }

    public void notifyAdminOfNewPayment(Payment payment) {
        PaymentNotificationDto dto = new PaymentNotificationDto(
                payment.getId(),
                payment.getFinalUser().getName(),
                payment.getOffer().getPrice(),
                payment.getPaymentDate()
        );
        messagingTemplate.convertAndSend("/topic/admin/payments", dto);
    }

}
