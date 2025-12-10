package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.FcmTokenDto;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/fcm")
public class FcmTokenController {

    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;
    private final FirebaseMessaging firebaseMessaging;

    @PatchMapping ("/token")
    public ResponseEntity<Void> registerFcmToken(@Valid @RequestBody FcmTokenDto tokenDto, Authentication auth) {
        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROVIDER"))){
            if (!serviceProviderRepository.existsById(tokenDto.getUserId())){
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            serviceProviderRepository.updateFcmToken(tokenDto.getUserId(), tokenDto.getToken());
            log.info("Token FCM actualizado para el prestador de servicios ID: {}", tokenDto.getUserId());
            return ResponseEntity.ok().build();
        }
        if (!finalUserRepository.existsById(tokenDto.getUserId())){
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        finalUserRepository.updateFcmToken(tokenDto.getUserId(), tokenDto.getToken());
        log.info("Token FCM actualizado para el usuario ID: {}", tokenDto.getUserId());

        if (auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))){
            final String topic = "ADMIN_NEW_PAYMENTS";
            try {
                firebaseMessaging.subscribeToTopic(List.of(tokenDto.getToken()), topic);
                log.info("Token {} suscrito con éxito al tópico {}.", tokenDto.getToken(), topic);
            } catch (FirebaseMessagingException e) {
                log.error("Error al suscribir el token {} al tópico {}: {}", tokenDto.getToken(), topic, e.getMessage());
            }
        }
        return ResponseEntity.ok().build();
    }
}
