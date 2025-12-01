package app.bys.bys_api.service;

import app.bys.bys_api.error.ForbiddenActionException;
import app.bys.bys_api.mapper.NotificationMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.NotificationDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.entity.*;
import app.bys.bys_api.model.enums.NotificationType;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.repository.*;
import app.bys.bys_api.service.specification.NotificationSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
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
        Notification userNotification = Notification.builder()
                .finalUser(finalUser)
                .read(false)
                .serviceRequest(serviceRequest)
                .timestamp(LocalDateTime.now())
                .notificationType(NotificationType.PAYMENT_ACCEPTED)
                .build();

        notificationRepository.save(userNotification);

        String notificationTitle = "Pago aceptado";
        String notificationBody = " Usuario: " + finalUser.getId();

        String fcmToken = finalUser.getFcmToken();

        Map<String, String> dataPayload = Map.of(
                "entityType", "request",
                "entityId", String.valueOf(serviceRequest.getId()),
                "status", "accepted"
        );

        try {
            fcmService.sendNotification(fcmToken, notificationTitle, notificationBody, dataPayload);
        } catch (FirebaseMessagingException e) {
            log.error("Error al enviar FCM al token {} del usuario {}: {}", fcmToken, finalUser.getId(), e.getMessage());

            // Si el token es inválido o no registrado, limpiar el token en la DB
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                finalUserRepository.updateFcmToken(finalUser.getId(), null);
                log.warn("Token FCM limpiado para el FinalUser ID {} debido a UNREGISTERED.", finalUser.getId());
            }
        }
    }

    private void notifyProvider(ServiceProvider serviceProvider, ServiceRequest serviceRequest, Offer offer) {

        Notification providerNotification = Notification.builder()
                .serviceProvider(serviceProvider)
                .read(false)
                .serviceRequest(serviceRequest)
                .offer(offer)
                .timestamp(LocalDateTime.now())
                .notificationType(NotificationType.PAID_OFFER)
                .build();

        notificationRepository.save(providerNotification);

        String notificationTitle = "Su oferta ha sido pagada";
        String notificationBody = " Prestador de Servicios: " + serviceProvider.getId();

        String fcmToken = serviceProvider.getFcmToken();

        Map<String, String> dataPayload = Map.of(
                "entityType", "request",
                "entityId", String.valueOf(serviceRequest.getId()),
                "status", "accepted"
        );
        try {
            fcmService.sendNotification(fcmToken, notificationTitle, notificationBody, dataPayload);
        } catch (FirebaseMessagingException e) {
            log.error("Error al enviar FCM al token {} del usuario {}: {}", fcmToken, serviceProvider.getId(), e.getMessage());

            // Si el token es inválido o no registrado, limpiar el token en la DB
            if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED) {
                serviceProviderRepository.updateFcmToken(serviceProvider.getId(), null);
                log.warn("Token FCM limpiado para el FinalUser ID {} debido a UNREGISTERED.", serviceProvider.getId());
            }
        }
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
                    "entityType", "request",
                    "entityId", String.valueOf(requestId),
                    "status", "pending"
            );
            sendFcmNotification(token, notificationTitle, notificationBody, dataPayload);
        }
    }

    private void sendFcmNotification(String token, String notificationTitle, String notificationBody, Map<String, String> dataPayload){
        try {
            fcmService.sendNotification(token, notificationTitle, notificationBody, dataPayload);
        } catch (FirebaseMessagingException e) {
            log.error("Error al enviar FCM al token {}: {}", token, e.getMessage());
            // Lógica para marcar el token como inválido en la DB.
        }
    }
}


