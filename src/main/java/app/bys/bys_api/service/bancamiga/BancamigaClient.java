package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.model.dto.bancamiga.BancamigaFindPaymentResponse;
import app.bys.bys_api.model.dto.bancamiga.BancamigaTokenResponse;

import java.time.LocalDate;

/**
 * Cliente de la API de conciliacion de Pago Movil de Bancamiga. Ver doc "Bancamiga 3rd party APIs".
 * Todas las implementaciones deben lanzar exclusivamente las excepciones de app.bys.bys_api.error.Bancamiga*
 * (nunca dejar propagar RestClientException u otras excepciones de bajo nivel).
 */
public interface BancamigaClient {

    boolean healthcheck();

    BancamigaFindPaymentResponse findPaymentMobile(String phone, String bankCode, LocalDate date);

    BancamigaTokenResponse refreshToken();
}
