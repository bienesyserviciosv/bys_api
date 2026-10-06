package app.bys.bys_api.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Barrido semanal que borra definitivamente los registros transaccionales conservados de
 * cuentas eliminadas cuando vence el plazo de la politica de privacidad
 * (AccountDeletionService.RETENTION_YEARS anios desde la fecha del pago).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RetainedRecordsPurgeScheduler {

    private final AccountDeletionService accountDeletionService;

    // Domingos 03:30 (hora del servidor).
    @Scheduled(cron = "0 30 3 * * SUN")
    public void purgeExpiredRecords() {
        try {
            accountDeletionService.purgeExpiredRetainedRecords();
        } catch (Exception e) {
            // Un fallo no debe romper el scheduler: se reintenta en la siguiente corrida.
            log.error("Fallo el barrido de registros conservados vencidos", e);
        }
    }
}
