package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.FcmTokenDto;
import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.service.FcmTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
@RequestMapping("/fcm")
public class FcmTokenController {

    private final FcmTokenService fcmTokenService;

    @PatchMapping("/token")
    public ResponseEntity<Void> registerFcmToken(@Valid @RequestBody FcmTokenDto tokenDto, Authentication auth) {
        Set<Role> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .map(name -> {
                    Role role = new Role();
                    role.setName(name);
                    return role;
                })
                .collect(Collectors.toSet());

        fcmTokenService.registerFcmToken(tokenDto.getUserId(), tokenDto.getToken(), roles);
        return ResponseEntity.ok().build();
    }
}
