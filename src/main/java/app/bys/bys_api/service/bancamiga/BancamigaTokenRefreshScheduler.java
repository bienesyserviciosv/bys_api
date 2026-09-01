package app.bys.bys_api.service.bancamiga;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BancamigaTokenRefreshScheduler {

    private final BancamigaTokenService bancamigaTokenService;

    @Value("${bancamiga.enabled}")
    private boolean bancamigaEnabled;

    @Scheduled(cron = "${bancamiga.token.refresh-cron}")
    public void refreshTokenIfDue() {
        if (!bancamigaEnabled) {
            log.debug("Bancamiga deshabilitado (bancamiga.enabled=false), se omite el refresh de token");
            return;
        }
        bancamigaTokenService.refreshIfDue();
    }
}
