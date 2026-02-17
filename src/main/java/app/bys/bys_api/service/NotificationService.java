package app.bys.bys_api.service;

import app.bys.bys_api.error.ConflictException;
import app.bys.bys_api.mapper.NotificationMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.dto.NotificationRequest;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.*;
import app.bys.bys_api.model.entity.Notification;
import app.bys.bys_api.model.enums.NotificationType;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.repository.*;
import app.bys.bys_api.service.specification.NotificationSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import com.google.firebase.messaging.*;
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
    private final OfferRepository offerRepository;
    private final FCMService fcmService;

    public void notifyProvidersOfNewRequest(Long specializationId, ServiceRequest serviceRequest) {

        List<ServiceProvider> providers = serviceProviderRepository.findBySpecializations_Id(specializationId);

        if (providers == null || providers.isEmpty()) {
            log.debug("No providers to notify for specialization {}", specializationId);
            return;
        }

        List<Notification> notificationsToSend = new ArrayList<>();

        for (ServiceProvider provider : providers) {
            if (!notificationRepository.existsByServiceProviderAndServiceRequestAndNotificationType(
                    provider, serviceRequest, NotificationType.NEW_REQUEST)) {

                Notification notification = Notification.builder()
                        .serviceProvider(provider)
                        .read(false)
                        .timestamp(LocalDateTime.now())
                        .serviceRequest(serviceRequest)
                        .notificationType(NotificationType.NEW_REQUEST)
                        .build();

                try {
                    Notification savedNotification = notificationRepository.save(notification);
                    notificationsToSend.add(savedNotification);
                    log.debug("Notification created for provider {}", provider.getId());
                } catch (Exception e) {
                    // Unique constraint violation or other database error
                    log.debug("Notification already exists or error saving for provider {}: {}", provider.getId(), e.getMessage());
                }
            } else {
                log.debug("Notification already exists for provider {}", provider.getId());
            }
        }

        log.info("{} notification(s) created", notificationsToSend.size());

        List<NotificationRequest> requests = new ArrayList<>();

        for (Notification notification : notificationsToSend) {
            ServiceProvider provider = notification.getServiceProvider();
            String fcmToken = provider.getFcmToken();

            if (fcmToken != null && !fcmToken.trim().isEmpty()) {
                Map<String, String> dataPayload = Map.of(
                        "notificationType", "NEW_REQUEST",
                        "targetEntityType", "provider",
                        "targetEntityId", String.valueOf(provider.getId()),
                        "relatedEntityType", "request",
                        "relatedEntityId", String.valueOf(serviceRequest.getId()),
                        "notificationId", String.valueOf(notification.getId()),
                        "action", "view_request"
                );

                requests.add(new NotificationRequest(
                        fcmToken,
                        "Nueva solicitud disponible",
                        "Hay una nueva solicitud disponible para tu especialización",
                        dataPayload
                ));
            }
        }

        if (!requests.isEmpty()) {
            try {
                BatchResponse response = fcmService.sendBatchNotifications(requests);
                for (int i = 0; i < response.getResponses().size(); i++) {
                    SendResponse resp = response.getResponses().get(i);
                    if (!resp.isSuccessful()) {
                        MessagingErrorCode errorCode = resp.getException().getMessagingErrorCode();
                        // Handle multiple error codes that indicate invalid tokens
                        if (errorCode == MessagingErrorCode.UNREGISTERED ||
                            errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                            ServiceProvider provider = notificationsToSend.get(i).getServiceProvider();
                            serviceProviderRepository.updateFcmToken(provider.getId(), null);
                            log.warn("FCM Token cleared for provider ID {} due to {}.", provider.getId(), errorCode);
                        } else {
                            log.error("FCM send failed for provider ID {} with error: {}",
                                    notificationsToSend.get(i).getServiceProvider().getId(),
                                    resp.getException().getMessage());
                        }
                    }
                }
            } catch (FirebaseMessagingException e) {
                log.error("Error sending batch FCM notifications: {}", e.getMessage());
            }
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

        throw new ConflictException("The user can't read this notification");
    }

    public void notifyPaymentAccepted(Long userId, Long providerId, Long requestId, Long offerId) {
        FinalUser finalUser = finalUserRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Final user with id: " + userId + " not found"));

        ServiceProvider serviceProvider = serviceProviderRepository.findById(providerId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceProvider with id: " + providerId + " not found"));

        ServiceRequest serviceRequest = serviceRequestRepository.findById(requestId)
                .orElseThrow(() -> new EntityNotFoundException("ServiceRequest with id: " + requestId + " not found"));

        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + offerId + " not found"));

        notifyProvider(serviceProvider, serviceRequest, offer);
        notifyUser(finalUser, serviceRequest);

    }

    private void notifyUser(FinalUser finalUser, ServiceRequest serviceRequest) {
        try {
            if (notificationRepository.existsByFinalUserAndServiceRequestAndNotificationType(
                    finalUser, serviceRequest, NotificationType.PAYMENT_ACCEPTED)) {
                log.debug("User {} already has PAYMENT_ACCEPTED notification for request {}", finalUser.getId(), serviceRequest.getId());
                return;
            }

            Notification userNotification = Notification.builder()
                    .finalUser(finalUser)
                    .read(false)
                    .serviceRequest(serviceRequest)
                    .timestamp(LocalDateTime.now())
                    .notificationType(NotificationType.PAYMENT_ACCEPTED)
                    .build();

            Notification savedNotification = notificationRepository.save(userNotification);

            String fcmToken = finalUser.getFcmToken();

            if (fcmToken != null && !fcmToken.trim().isEmpty()) {
                String notificationTitle = "Pago aceptado";
                String notificationBody = " Tu pago ha sido aceptado para la solicitud: " + serviceRequest.getId();

                Map<String, String> dataPayload = Map.of(
                        "notificationType", "PAYMENT_ACCEPTED",
                        "targetEntityType", "user",
                        "targetEntityId", String.valueOf(finalUser.getId()),
                        "relatedEntityType", "request",
                        "relatedEntityId", String.valueOf(serviceRequest.getId()),
                        "notificationId", String.valueOf(savedNotification.getId()),
                        "action", "view_payment_status"
                );
                sendFcmNotification(fcmToken, notificationTitle, notificationBody, dataPayload, "user", finalUser.getId());
            } else {
                log.warn("Usuario {} no tiene token FCM registrado, no se puede enviar notificación", finalUser.getId());
            }
        } catch (Exception e) {
            // Unique constraint violation or other database error
            log.debug("PAYMENT_ACCEPTED notification already exists or error saving for user {}: {}", finalUser.getId(), e.getMessage());
        }
    }

    private void notifyProvider(ServiceProvider serviceProvider, ServiceRequest serviceRequest, Offer offer) {
        // Try to create notification only if it doesn't exist
        try {
            if (notificationRepository.existsByServiceProviderAndServiceRequestAndNotificationType(
                    serviceProvider, serviceRequest, NotificationType.PAID_OFFER)) {
                log.debug("Provider {} already has PAID_OFFER notification for request {}", serviceProvider.getId(), serviceRequest.getId());
                return; // Salir si ya existe
            }

            Notification providerNotification = Notification.builder()
                    .serviceProvider(serviceProvider)
                    .read(false)
                    .serviceRequest(serviceRequest)
                    .offer(offer)
                    .timestamp(LocalDateTime.now())
                    .notificationType(NotificationType.PAID_OFFER)
                    .build();

            Notification savedNotification = notificationRepository.save(providerNotification);

            String fcmToken = serviceProvider.getFcmToken();

            if (fcmToken != null && !fcmToken.trim().isEmpty()) {
                String notificationTitle = "Su oferta ha sido pagada";
                String notificationBody = " Prestador de Servicios: " + serviceProvider.getId();

                Map<String, String> dataPayload = Map.of(
                        "notificationType", "PAID_OFFER",
                        "targetEntityType", "provider",
                        "targetEntityId", String.valueOf(serviceProvider.getId()),
                        "relatedEntityType", "request",
                        "relatedEntityId", String.valueOf(serviceRequest.getId()),
                        "relatedEntityType2", "offer",
                        "relatedEntityId2", String.valueOf(offer.getId()),
                        "notificationId", String.valueOf(savedNotification.getId()),
                        "action", "view_paid_offer"
                );
                sendFcmNotification(fcmToken, notificationTitle, notificationBody, dataPayload, "provider", serviceProvider.getId());
            } else {
                log.warn("Proveedor {} no tiene token FCM registrado, no se puede enviar notificación", serviceProvider.getId());
            }
        } catch (Exception e) {
            // Unique constraint violation or other database error
            log.debug("PAID_OFFER notification already exists or error saving for provider {}: {}", serviceProvider.getId(), e.getMessage());
        }
    }


    public void notifyAdminOfNewPayment(Long userId, Long providerId, Long requestId, PaymentType paymentType) {
        String notificationTitle = "";
        switch (paymentType) {
            case MOBILE -> notificationTitle = "Nuevo Pago Móvil PENDIENTE";
            case TRANSFER -> notificationTitle = "Nuevo Pago por Transferencia PENDIENTE";
        }
        String notificationBody = "Solicitud: " + requestId + " Usuario: " + userId + " Proveedor: " + providerId;

        Map<String, String> dataPayload = Map.of(
                "notificationType", "NEW_PAYMENT",
                "targetEntityType", "admin",
                "relatedEntityType", "request",
                "relatedEntityId", String.valueOf(requestId),
                "relatedEntityType2", "user",
                "relatedEntityId2", String.valueOf(userId),
                "relatedEntityType3", "provider",
                "relatedEntityId3", String.valueOf(providerId),
                "paymentType", paymentType.toString(),
                "action", "review_payment"
        );

        final String topicName = "ADMIN_NEW_PAYMENTS";

        try {
            fcmService.sendTopicNotification(topicName, notificationTitle, notificationBody, dataPayload);
        } catch (FirebaseMessagingException e) {
            log.error("Error al enviar notificación al Tópico {}: {}", topicName, e.getMessage());
        }
    }

    private void sendFcmNotification(String token, String notificationTitle, String notificationBody, Map<String, String> dataPayload, String entityType, Long entityId) {
        if (token == null || token.trim().isEmpty()) {
            log.warn("Token FCM es null o vacío para la entidad {}, no se puede enviar notificación", entityType + " ID " + entityId);
            return;
        }

        try {
            fcmService.sendNotification(token, notificationTitle, notificationBody, dataPayload);
            log.debug("FCM notification sent successfully to {} ID {}", entityType, entityId);
        } catch (FirebaseMessagingException e) {
            MessagingErrorCode errorCode = e.getMessagingErrorCode();
            log.error("Error al enviar FCM al token {} del {}: {} (Error Code: {})", token, entityType + " ID " + entityId, e.getMessage(), errorCode);

            // Clear token for various invalid token errors
            if (errorCode == MessagingErrorCode.UNREGISTERED ||
                errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                if (entityType.equals("provider")) {
                    serviceProviderRepository.updateFcmToken(entityId, null);
                    log.warn("Token FCM limpiado para ServiceProvider ID {} debido a {}.", entityId, errorCode);
                } else if (entityType.equals("user")) {
                    finalUserRepository.updateFcmToken(entityId, null);
                    log.warn("Token FCM limpiado para FinalUser ID {} debido a {}.", entityId, errorCode);
                }
            }
        }
    }
}