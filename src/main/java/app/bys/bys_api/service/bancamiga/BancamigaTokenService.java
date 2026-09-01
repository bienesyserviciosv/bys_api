package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.model.dto.bancamiga.BancamigaTokenResponse;
import app.bys.bys_api.model.entity.BancamigaCredential;
import app.bys.bys_api.repository.BancamigaCredentialRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@RequiredArgsConstructor
public class BancamigaTokenService {

    private final BancamigaCredentialRepository credentialRepository;
    private final BancamigaClient bancamigaClient;

    @Value("${bancamiga.token.refresh-min-interval-days}")
    private int refreshMinIntervalDays;

    public BancamigaCredential seedCredentials(String accessToken, String refreshToken) {
        BancamigaCredential credential = credentialRepository.findById(1L).orElseGet(BancamigaCredential::new);
        LocalDateTime now = LocalDateTime.now();

        credential.setAccessToken(accessToken);
        credential.setRefreshToken(refreshToken);
        credential.setTokenIssuedAt(now);
        credential.setLastRefreshedAt(now);
        credential.setLastRefreshError(null);
        if (credential.getCreatedAt() == null) {
            credential.setCreatedAt(now);
        }
        credential.setUpdatedAt(now);

        return credentialRepository.save(credential);
    }

    public BancamigaCredential getStatus() {
        return credentialRepository.findById(1L).orElse(null);
    }

    public void refreshIfDue() {
        BancamigaCredential credential = credentialRepository.findById(1L).orElse(null);
        if (credential == null || credential.getAccessToken() == null) {
            log.debug("No hay credenciales de Bancamiga sembradas todavia, se omite el refresh");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (credential.getLastRefreshedAt() != null
                && credential.getLastRefreshedAt().isAfter(now.minusDays(refreshMinIntervalDays))) {
            log.debug("Token de Bancamiga refrescado recientemente ({}), se omite este ciclo", credential.getLastRefreshedAt());
            return;
        }

        credential.setLastRefreshAttemptAt(now);

        try {
            BancamigaTokenResponse response = bancamigaClient.refreshToken();
            credential.setAccessToken(response.getToken());
            credential.setRefreshToken(response.getRefreshToken());
            credential.setTokenIssuedAt(now);
            if (response.getExpireDate() != null) {
                credential.setTokenExpiresAt(
                        LocalDateTime.ofInstant(Instant.ofEpochSecond(response.getExpireDate()), ZoneId.systemDefault()));
            }
            credential.setLastRefreshedAt(now);
            credential.setLastRefreshError(null);
            log.info("Token de Bancamiga renovado exitosamente");
        } catch (Exception e) {
            credential.setLastRefreshError(e.getMessage());
            log.error("Error renovando el token de Bancamiga: {}", e.getMessage(), e);
        } finally {
            credential.setUpdatedAt(LocalDateTime.now());
            credentialRepository.save(credential);
        }
    }
}
