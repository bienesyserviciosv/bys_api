package app.bys.bys_api.service.c2p;

import app.bys.bys_api.config.BancamigaC2pRestClient;
import app.bys.bys_api.error.BancamigaTimeoutException;
import app.bys.bys_api.error.BancamigaTokenExpiredException;
import app.bys.bys_api.error.BancamigaUnavailableException;
import app.bys.bys_api.error.BancamigaValidationException;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pBanksResponse;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pMovementsRequest;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pMovementsResponse;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pPaymentRequest;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pPaymentResponse;
import app.bys.bys_api.model.dto.bancamiga.BancamigaErrorBody;
import app.bys.bys_api.repository.BancamigaCredentialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Cliente de la API C2P de Bancamiga (doc "Doc API Bancamiga - PMC2P"). Usa el mismo access token
 * que la conciliacion de Pago Movil (tabla bancamiga_credential).
 * Solo lanza excepciones app.bys.bys_api.error.Bancamiga*. Nunca registra la OTP.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BancamigaC2pClient {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final BancamigaC2pRestClient c2pRestClient;
    private final BancamigaCredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;

    /** GET /public/protected/c2p/banks */
    public BancamigaC2pBanksResponse listBanks() {
        try {
            return c2pRestClient.client().get()
                    .uri("/public/protected/c2p/banks")
                    .header("Authorization", "Bearer " + currentAccessToken())
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), this::handleErrorResponse)
                    .body(BancamigaC2pBanksResponse.class);
        } catch (RestClientException e) {
            throw new BancamigaUnavailableException("Error consultando c2p/banks en Bancamiga: " + e.getMessage(), e);
        }
    }

    /**
     * POST /public/protected/c2p/payments/v2.
     * Lanza BancamigaTimeoutException si el cobro se envio pero no hubo respuesta (resultado desconocido):
     * en ese caso NO se debe reintentar, se resuelve por conciliacion.
     */
    public BancamigaC2pPaymentResponse pay(BancamigaC2pPaymentRequest request) {
        String accessToken = currentAccessToken();
        try {
            return c2pRestClient.client().post()
                    .uri("/public/protected/c2p/payments/v2")
                    .header("Authorization", "Bearer " + accessToken)
                    .body(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), this::handleErrorResponse)
                    .body(BancamigaC2pPaymentResponse.class);
        } catch (ResourceAccessException e) {
            if (wasNeverSent(e)) {
                throw new BancamigaUnavailableException("No se pudo conectar con Bancamiga para el cobro C2P: " + e.getMessage(), e);
            }
            throw new BancamigaTimeoutException("Sin respuesta de Bancamiga al cobro C2P: " + e.getMessage(), e);
        } catch (RestClientException e) {
            throw new BancamigaUnavailableException("Error en c2p/payments de Bancamiga: " + e.getMessage(), e);
        }
    }

    /** POST /public/protected/c2p/mov: cobros C2P recibidos en una fecha (conciliacion). */
    public BancamigaC2pMovementsResponse movements(LocalDate date) {
        try {
            return c2pRestClient.client().post()
                    .uri("/public/protected/c2p/mov")
                    .header("Authorization", "Bearer " + currentAccessToken())
                    .body(new BancamigaC2pMovementsRequest(date.format(DATE_FORMAT)))
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), this::handleErrorResponse)
                    .body(BancamigaC2pMovementsResponse.class);
        } catch (RestClientException e) {
            throw new BancamigaUnavailableException("Error consultando c2p/mov en Bancamiga: " + e.getMessage(), e);
        }
    }

    private String currentAccessToken() {
        try {
            return credentialRepository.findSingletonOrThrow().getAccessToken();
        } catch (IllegalStateException e) {
            throw new BancamigaUnavailableException(e.getMessage());
        }
    }

    /**
     * true si la excepcion ocurrio antes de que la peticion llegara a Bancamiga
     * (no se resolvio el host, no se pudo conectar o se agoto el tiempo de conexion).
     */
    private static boolean wasNeverSent(Throwable e) {
        Throwable cause = e;
        while (cause != null) {
            if (cause instanceof ConnectException || cause instanceof UnknownHostException
                    || cause instanceof NoRouteToHostException) {
                return true;
            }
            if (cause instanceof SocketTimeoutException && cause.getMessage() != null
                    && cause.getMessage().toLowerCase().contains("connect")) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private void handleErrorResponse(HttpRequest request, ClientHttpResponse response) throws IOException {
        int httpStatus = response.getStatusCode().value();
        BancamigaErrorBody errorBody = tryParseErrorBody(response);

        int code = errorBody != null && errorBody.getCode() != null ? errorBody.getCode()
                : errorBody != null && errorBody.getStatus() != null ? errorBody.getStatus()
                : httpStatus;
        String message = errorBody != null && errorBody.getMensaje() != null ? errorBody.getMensaje()
                : errorBody != null && errorBody.getTitle() != null ? errorBody.getTitle()
                : "HTTP " + httpStatus;

        throw switch (code) {
            case 512 -> new BancamigaTokenExpiredException(message);
            case 511, 550, 551, 552, 553 -> new BancamigaValidationException(message);
            default -> new BancamigaUnavailableException("Bancamiga error " + code + ": " + message);
        };
    }

    private BancamigaErrorBody tryParseErrorBody(ClientHttpResponse response) {
        try {
            return objectMapper.readValue(response.getBody(), BancamigaErrorBody.class);
        } catch (IOException e) {
            log.warn("No se pudo parsear el body de error de Bancamiga: {}", e.getMessage());
            return null;
        }
    }
}
