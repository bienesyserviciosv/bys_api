package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.bancamiga.BancamigaWebhookPayload;
import app.bys.bys_api.model.dto.bancamiga.BancamigaWebhookResponse;
import app.bys.bys_api.service.bancamiga.BancamigaVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Recibe las notificaciones push de pago móvil de Bancamiga. Queda fuera del filtro JWT de la app
 * (ver SecurityConfig, path permitAll) porque el llamante es un integrador externo, no un usuario de
 * la app -- la autenticación se hace aquí mismo comparando el Bearer contra un secreto propio.
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/bancamiga")
public class BancamigaWebhookController {

    private final BancamigaVerificationService bancamigaVerificationService;

    @Value("${bancamiga.webhook.secret}")
    private String webhookSecret;

    @PostMapping("/webhook")
    public ResponseEntity<BancamigaWebhookResponse> receiveWebhook(
            @RequestHeader(name = "Authorization", required = false) String authorization,
            @RequestBody BancamigaWebhookPayload payload,
            HttpServletRequest request) {

        if (!isAuthorized(authorization)) {
            log.warn("Intento de acceso no autorizado al webhook de Bancamiga desde {}", request.getRemoteAddr());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        bancamigaVerificationService.handleWebhookMovement(payload);
        return ResponseEntity.ok(new BancamigaWebhookResponse(200, payload.getRefpk()));
    }

    private boolean isAuthorized(String authorizationHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            log.error("bancamiga.webhook.secret no esta configurado, se rechaza toda solicitud al webhook");
            return false;
        }
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return false;
        }

        byte[] provided = authorizationHeader.substring("Bearer ".length()).getBytes(StandardCharsets.UTF_8);
        byte[] expected = webhookSecret.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(provided, expected);
    }
}
