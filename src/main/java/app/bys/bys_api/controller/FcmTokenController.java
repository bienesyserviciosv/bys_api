package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.FcmTokenDto;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
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

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/fcm")
public class FcmTokenController {

    private final FinalUserRepository finalUserRepository;
    private final ServiceProviderRepository serviceProviderRepository;

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

        return ResponseEntity.ok().build();
    }
}
