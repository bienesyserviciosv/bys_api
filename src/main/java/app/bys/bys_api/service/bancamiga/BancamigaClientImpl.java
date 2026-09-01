package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.error.BancamigaTokenExpiredException;
import app.bys.bys_api.error.BancamigaUnavailableException;
import app.bys.bys_api.error.BancamigaValidationException;
import app.bys.bys_api.model.dto.bancamiga.*;
import app.bys.bys_api.model.entity.BancamigaCredential;
import app.bys.bys_api.repository.BancamigaCredentialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BancamigaClientImpl implements BancamigaClient {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;

    private final RestClient bancamigaRestClient;
    private final BancamigaCredentialRepository credentialRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean healthcheck() {
        try {
            Map<?, ?> response = bancamigaRestClient.get()
                    .uri("/healthcheck")
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), this::handleErrorResponse)
                    .body(Map.class);
            return response != null && Integer.valueOf(200).equals(response.get("code"));
        } catch (RestClientException e) {
            throw new BancamigaUnavailableException("No se pudo consultar el healthcheck de Bancamiga: " + e.getMessage(), e);
        }
    }

    @Override
    public BancamigaFindPaymentResponse findPaymentMobile(String phone, String bankCode, LocalDate date) {
        if (bankCode == null) {
            throw new BancamigaValidationException("No hay codigo de banco Bancamiga mapeado para este banco");
        }

        BancamigaFindPaymentRequest request = BancamigaFindPaymentRequest.builder()
                .phone(phone)
                .bank(bankCode)
                .date(date.format(DATE_FORMAT))
                .build();

        try {
            return bancamigaRestClient.post()
                    .uri("/public/protected/pm/find")
                    .header("Authorization", "Bearer " + currentAccessToken())
                    .body(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), this::handleErrorResponse)
                    .body(BancamigaFindPaymentResponse.class);
        } catch (RestClientException e) {
            throw new BancamigaUnavailableException("Error consultando pm/find en Bancamiga: " + e.getMessage(), e);
        }
    }

    @Override
    public BancamigaTokenResponse refreshToken() {
        BancamigaCredential credential = credentialRepository.findSingletonOrThrow();
        BancamigaRefreshRequest request = new BancamigaRefreshRequest(credential.getRefreshToken());

        try {
            return bancamigaRestClient.post()
                    .uri("/public/re/refresh")
                    .header("Authorization", "Bearer " + credential.getAccessToken())
                    .body(request)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), this::handleErrorResponse)
                    .body(BancamigaTokenResponse.class);
        } catch (RestClientException e) {
            throw new BancamigaUnavailableException("Error renovando el token de Bancamiga: " + e.getMessage(), e);
        }
    }

    private String currentAccessToken() {
        return credentialRepository.findSingletonOrThrow().getAccessToken();
    }

    private void handleErrorResponse(org.springframework.http.HttpRequest request, ClientHttpResponse response) throws IOException {
        int httpStatus = response.getStatusCode().value();
        BancamigaErrorBody errorBody = tryParseErrorBody(response);

        int code = errorBody != null && errorBody.getCode() != null ? errorBody.getCode()
                : errorBody != null && errorBody.getStatus() != null ? errorBody.getStatus()
                : httpStatus;
        String message = errorBody != null && errorBody.getMensaje() != null ? errorBody.getMensaje()
                : errorBody != null && errorBody.getTitle() != null ? errorBody.getTitle()
                : "HTTP " + httpStatus;

        throw mapErrorCode(code, message);
    }

    // Bancamiga documenta estos códigos como parte del body/status de sus respuestas de error
    // (511/550/551/552/553 formato, 512 token expirado, 503 credenciales/token no autorizado).
    // No se ha podido verificar el shape exacto contra el host real (interconexión pendiente).
    private RuntimeException mapErrorCode(int code, String message) {
        return switch (code) {
            case 512 -> new BancamigaTokenExpiredException(message);
            case 511, 550, 551, 552, 553 -> new BancamigaValidationException(message);
            case 503 -> new BancamigaUnavailableException(message);
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
