package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.model.dto.bancamiga.BancamigaTokenResponse;
import app.bys.bys_api.model.entity.BancamigaCredential;
import app.bys.bys_api.repository.BancamigaCredentialRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BancamigaTokenServiceTest {

    @Mock
    private BancamigaCredentialRepository credentialRepository;
    @Mock
    private BancamigaClient bancamigaClient;

    private BancamigaTokenService tokenService;

    @BeforeEach
    void setUp() {
        tokenService = new BancamigaTokenService(credentialRepository, bancamigaClient);
        ReflectionTestUtils.setField(tokenService, "refreshMinIntervalDays", 25);
    }

    @Test
    void seedCredentials_createsNewRow_whenNoneExists() {
        when(credentialRepository.findById(1L)).thenReturn(Optional.empty());
        when(credentialRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BancamigaCredential result = tokenService.seedCredentials("access-1", "refresh-1");

        assertThat(result.getAccessToken()).isEqualTo("access-1");
        assertThat(result.getRefreshToken()).isEqualTo("refresh-1");
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getLastRefreshedAt()).isNotNull();
    }

    @Test
    void refreshIfDue_doesNothing_whenNoCredentialsYet() {
        when(credentialRepository.findById(1L)).thenReturn(Optional.empty());

        tokenService.refreshIfDue();

        verifyNoInteractions(bancamigaClient);
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void refreshIfDue_skips_whenRefreshedRecently() {
        BancamigaCredential credential = new BancamigaCredential();
        credential.setAccessToken("token");
        credential.setLastRefreshedAt(LocalDateTime.now().minusDays(1));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential));

        tokenService.refreshIfDue();

        verifyNoInteractions(bancamigaClient);
        verify(credentialRepository, never()).save(any());
    }

    @Test
    void refreshIfDue_refreshesAndPersistsNewTokens_whenDue() {
        BancamigaCredential credential = new BancamigaCredential();
        credential.setAccessToken("old-token");
        credential.setRefreshToken("old-refresh");
        credential.setLastRefreshedAt(LocalDateTime.now().minusDays(30));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential));

        BancamigaTokenResponse response = new BancamigaTokenResponse();
        response.setToken("new-token");
        response.setRefreshToken("new-refresh");
        response.setExpireDate(1778246388L);
        when(bancamigaClient.refreshToken()).thenReturn(response);

        tokenService.refreshIfDue();

        verify(credentialRepository).save(argThat(c ->
                "new-token".equals(c.getAccessToken())
                        && "new-refresh".equals(c.getRefreshToken())
                        && c.getTokenExpiresAt() != null
                        && c.getLastRefreshError() == null));
    }

    @Test
    void refreshIfDue_recordsError_whenBancamigaClientFails() {
        BancamigaCredential credential = new BancamigaCredential();
        credential.setAccessToken("old-token");
        credential.setLastRefreshedAt(LocalDateTime.now().minusDays(30));
        when(credentialRepository.findById(1L)).thenReturn(Optional.of(credential));
        when(bancamigaClient.refreshToken()).thenThrow(new RuntimeException("Bancamiga no disponible"));

        tokenService.refreshIfDue();

        verify(credentialRepository).save(argThat(c -> "Bancamiga no disponible".equals(c.getLastRefreshError())));
    }
}
