package app.bys.bys_api.service;

import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    private static final String ADMIN_PAYMENTS_TOPIC = "ADMIN_NEW_PAYMENTS";

    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final FirebaseMessaging firebaseMessaging;

    public void registerFcmToken(Long entityId, String fcmToken, Set<Role> roles) {
        if (fcmToken == null || fcmToken.isBlank()) {
            return;
        }

        String token = fcmToken.trim();

        if (roles.stream().anyMatch(role -> "ROLE_PROVIDER".equals(role.getName()))) {
            if (!serviceProviderRepository.existsById(entityId)) {
                throw new EntityNotFoundException("Provider with id " + entityId + " not found");
            }
            serviceProviderRepository.updateFcmToken(entityId, token);
            log.info("FCM token updated for service provider ID: {}", entityId);
            return;
        }

        if (roles.stream().anyMatch(role -> "ROLE_USER".equals(role.getName()))) {
            if (!finalUserRepository.existsById(entityId)) {
                throw new EntityNotFoundException("Final User with id " + entityId + " not found");
            }
            finalUserRepository.updateFcmToken(entityId, token);
            log.info("FCM token updated for final user ID: {}", entityId);
        }

        if (roles.stream().anyMatch(role ->
                "ROLE_ADMIN".equals(role.getName()) || "ROLE_SUPER_ADMIN".equals(role.getName()))) {
            subscribeAdminToPaymentsTopic(token);
        }
    }

    private void subscribeAdminToPaymentsTopic(String token) {
        try {
            firebaseMessaging.subscribeToTopic(List.of(token), ADMIN_PAYMENTS_TOPIC);
            log.info("FCM token subscribed to topic {}", ADMIN_PAYMENTS_TOPIC);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to subscribe token to topic {}: {}", ADMIN_PAYMENTS_TOPIC, e.getMessage());
        }
    }
}
