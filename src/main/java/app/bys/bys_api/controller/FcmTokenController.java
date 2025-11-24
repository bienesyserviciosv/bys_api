package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.FcmTokenDto;
import app.bys.bys_api.repository.FinalUserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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

    @PatchMapping ("/token")
    public ResponseEntity<Void> registerFcmToken(@Valid @RequestBody FcmTokenDto tokenDto) {

        finalUserRepository.updateFcmToken(tokenDto.getUserId(), tokenDto.getToken());
        log.info("Token FCM actualizado para el usuario ID: {}", tokenDto.getUserId());

        return ResponseEntity.ok().build();
    }
}
