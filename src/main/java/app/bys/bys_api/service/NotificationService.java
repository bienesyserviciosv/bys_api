package app.bys.bys_api.service;

import app.bys.bys_api.error.ForbiddenActionException;
import app.bys.bys_api.mapper.NotificationMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Notification;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.repository.*;
import app.bys.bys_api.service.specification.NotificationSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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
    private final FCMService fcmService;

    public void notifyProvidersOfNewRequest(Long specializationId, ServiceRequest serviceRequest) {

        List<ServiceProvider> providers = serviceProviderRepository.findBySpecializations_Id(specializationId);

        if (providers != null && !providers.isEmpty()) {
            List<Notification> notifications = providers.stream()
                    .map(provider -> Notification.builder()
                            .serviceProvider(provider)
                            .message("Nueva solicitud disponible")
                            .read(false)
                            .timestamp(LocalDateTime.now())
                            .serviceRequest(serviceRequest)
                            .build())
                    .collect(Collectors.toList());

            notificationRepository.saveAll(notifications);
            log.info("{} notification created", notifications.size());
        } else {
            log.warn("No providers found with this conditions");
        }
    }

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


    public void notifyPaymentAccepted(Long userId, Long providerId, Long requestId) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

        ServiceProvider serviceProvider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceProvider with id: " + providerId + " not found"));

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceRequest with id: " + requestId + " not found"));

        Notification providerNotification = Notification.builder()
                .serviceProvider(serviceProvider)
                .message("Su oferta a la solicitud fue aceptada")
                .read(false)
                .serviceRequest(serviceRequest)
                .timestamp(LocalDateTime.now())
                .build();


        Notification userNotification = Notification.builder()
                .finalUser(finalUser)
                .message("Su solicitud fue aceptada")
                .read(false)
                .serviceRequest(serviceRequest)
                .timestamp(LocalDateTime.now())
                .build();

        notificationRepository.save(providerNotification);
        notificationRepository.save(userNotification);

    }

    public void notifyAdminOfNewPayment(Long userId, Long providerId, Long requestId, PaymentType paymentType) {
        String notificationTitle = "";
        switch (paymentType) {
            case MOBILE -> notificationTitle = "Nuevo Pago Móvil PENDIENTE";
            case TRANSFER -> notificationTitle = "Nuevo Pago por Transferencia PENDIENTE";
        }
        String notificationBody = "Solicitud: " + requestId + " Usuario: " + userId + " Proveedor: " + providerId;

        List<FinalUser> admins = finalUserRepository.findAdmins();
        List<String> fcmTokens = admins.stream().map(FinalUser::getFcmToken).toList();

        for (String token : fcmTokens) {
            Map<String, String> dataPayload = Map.of(
                    "entityType", "payment",
                    "entityId", String.valueOf(requestId),
                    "status", "pending"
            );
            try {
                fcmService.sendNotification(token, notificationTitle, notificationBody, dataPayload);
            } catch (FirebaseMessagingException e) {
                log.error("Error al enviar FCM al token {}: {}", token, e.getMessage());
                // Lógica para marcar el token como inválido en la DB.
            }
        }
    }

}
