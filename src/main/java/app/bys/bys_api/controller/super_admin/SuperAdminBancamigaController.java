package app.bys.bys_api.controller.super_admin;

import app.bys.bys_api.model.dto.bancamiga.BancamigaCredentialSeedDto;
import app.bys.bys_api.model.dto.bancamiga.BancamigaStatusDto;
import app.bys.bys_api.model.entity.BancamigaCredential;
import app.bys.bys_api.service.bancamiga.BancamigaTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/super_admin/bancamiga")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminBancamigaController {

    private final BancamigaTokenService bancamigaTokenService;

    @Value("${bancamiga.enabled}")
    private boolean bancamigaEnabled;

    @PostMapping("/credentials")
    public ResponseEntity<BancamigaStatusDto> seedCredentials(@Valid @RequestBody BancamigaCredentialSeedDto dto) {
        BancamigaCredential credential = bancamigaTokenService.seedCredentials(dto.getToken(), dto.getRefreshToken());
        return ResponseEntity.ok(toStatusDto(credential));
    }

    @GetMapping("/status")
    public ResponseEntity<BancamigaStatusDto> getStatus() {
        return ResponseEntity.ok(toStatusDto(bancamigaTokenService.getStatus()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<BancamigaStatusDto> forceRefresh() {
        bancamigaTokenService.refreshIfDue();
        return ResponseEntity.ok(toStatusDto(bancamigaTokenService.getStatus()));
    }

    private BancamigaStatusDto toStatusDto(BancamigaCredential credential) {
        return BancamigaStatusDto.builder()
                .enabled(bancamigaEnabled)
                .hasCredentials(credential != null && credential.getAccessToken() != null)
                .tokenIssuedAt(credential != null ? credential.getTokenIssuedAt() : null)
                .tokenExpiresAt(credential != null ? credential.getTokenExpiresAt() : null)
                .lastRefreshedAt(credential != null ? credential.getLastRefreshedAt() : null)
                .lastRefreshAttemptAt(credential != null ? credential.getLastRefreshAttemptAt() : null)
                .lastRefreshError(credential != null ? credential.getLastRefreshError() : null)
                .build();
    }
}
