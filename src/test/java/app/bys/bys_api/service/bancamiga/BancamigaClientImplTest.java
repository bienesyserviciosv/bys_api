package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.error.BancamigaTokenExpiredException;
import app.bys.bys_api.error.BancamigaUnavailableException;
import app.bys.bys_api.error.BancamigaValidationException;
import app.bys.bys_api.model.dto.bancamiga.BancamigaFindPaymentResponse;
import app.bys.bys_api.model.dto.bancamiga.BancamigaTokenResponse;
import app.bys.bys_api.model.entity.BancamigaCredential;
import app.bys.bys_api.repository.BancamigaCredentialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Prueba la serializacion/deserializacion y el mapeo de errores de BancamigaClientImpl contra un
 * servidor HTTP simulado (MockRestServiceServer) -- no depende del host real de Bancamiga.
 */
@ExtendWith(MockitoExtension.class)
class BancamigaClientImplTest {

    @Mock
    private BancamigaCredentialRepository credentialRepository;

    private MockRestServiceServer mockServer;
    private BancamigaClientImpl client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://bancamiga.test");
        mockServer = MockRestServiceServer.bindTo(builder).build();
        RestClient restClient = builder.build();

        client = new BancamigaClientImpl(restClient, credentialRepository, new ObjectMapper());
    }

    private BancamigaCredential stubCredential() {
        BancamigaCredential credential = new BancamigaCredential();
        credential.setAccessToken("current-access-token");
        credential.setRefreshToken("current-refresh-token");
        return credential;
    }

    @Test
    void healthcheck_returnsTrue_onCode200() {
        mockServer.expect(requestTo("http://bancamiga.test/healthcheck"))
                .andExpect(method(org.springframework.http.HttpMethod.GET))
                .andRespond(withSuccess("{\"code\":200,\"time\":\"2026-05-06T15:47:36-04:00\"}", MediaType.APPLICATION_JSON));

        assertThat(client.healthcheck()).isTrue();
        mockServer.verify();
    }

    @Test
    void findPaymentMobile_parsesMovementList_onSuccess() {
        when(credentialRepository.findSingletonOrThrow()).thenReturn(stubCredential());

        String body = """
                {
                  "code": 200,
                  "lista": [
                    {
                      "ID": "b6639593-7623-41c1-b1e0-4b041a64018f",
                      "Dni": "J000000000",
                      "PhoneDest": "584120000000",
                      "PhoneOrig": "584240000000",
                      "Amount": 5.12,
                      "BancoOrig": "0172",
                      "NroReferenciaCorto": "575202",
                      "NroReferencia": "000000575202",
                      "HoraMovimiento": "10:12:02",
                      "FechaMovimiento": "2026-05-06",
                      "Descripcion": "pago",
                      "Status": "500",
                      "Refpk": "202605060172584240000000575202",
                      "Ref": 29211968
                    }
                  ],
                  "mod": "find",
                  "num": 1
                }
                """;

        mockServer.expect(requestTo("http://bancamiga.test/public/protected/pm/find"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer current-access-token"))
                .andExpect(jsonPath("$.Phone").value("584240000000"))
                .andExpect(jsonPath("$.Bank").value("0172"))
                .andExpect(jsonPath("$.Date").value("2026-05-06"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        BancamigaFindPaymentResponse response = client.findPaymentMobile("584240000000", "0172", LocalDate.of(2026, 5, 6));

        assertThat(response.getNum()).isEqualTo(1);
        assertThat(response.getLista()).hasSize(1);
        assertThat(response.getLista().get(0).getAmount()).isEqualTo(5.12);
        assertThat(response.getLista().get(0).getRefpk()).isEqualTo("202605060172584240000000575202");
        mockServer.verify();
    }

    @Test
    void refreshToken_mapsRefresTokenTypoField() {
        BancamigaCredential credential = stubCredential();
        when(credentialRepository.findSingletonOrThrow()).thenReturn(credential);

        String body = """
                {
                  "code": 200,
                  "expireDate": 1778246388,
                  "mensaje": "Token generado exitosamente",
                  "mod": "users",
                  "refresToken": "new-refresh-token",
                  "token": "new-access-token"
                }
                """;

        mockServer.expect(requestTo("http://bancamiga.test/public/re/refresh"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer current-access-token"))
                .andExpect(jsonPath("$.refresh_token").value("current-refresh-token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

        BancamigaTokenResponse response = client.refreshToken();

        assertThat(response.getToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getExpireDate()).isEqualTo(1778246388L);
        mockServer.verify();
    }

    @Test
    void findPaymentMobile_mapsCode512ToTokenExpired() {
        when(credentialRepository.findSingletonOrThrow()).thenReturn(stubCredential());

        mockServer.expect(requestTo("http://bancamiga.test/public/protected/pm/find"))
                .andRespond(withStatus(HttpStatusCode.valueOf(512))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":512,\"mensaje\":\"Error token expirado o invalido\"}"));

        assertThatThrownBy(() -> client.findPaymentMobile("584240000000", "0172", LocalDate.of(2026, 5, 6)))
                .isInstanceOf(BancamigaTokenExpiredException.class)
                .hasMessageContaining("expirado");
    }

    @Test
    void findPaymentMobile_mapsCode511ToValidationError() {
        when(credentialRepository.findSingletonOrThrow()).thenReturn(stubCredential());

        mockServer.expect(requestTo("http://bancamiga.test/public/protected/pm/find"))
                .andRespond(withStatus(HttpStatusCode.valueOf(511))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":511,\"mensaje\":\"Error formato\"}"));

        assertThatThrownBy(() -> client.findPaymentMobile("584240000000", "0172", LocalDate.of(2026, 5, 6)))
                .isInstanceOf(BancamigaValidationException.class);
    }

    @Test
    void findPaymentMobile_mapsCode503ToUnavailable() {
        when(credentialRepository.findSingletonOrThrow()).thenReturn(stubCredential());

        mockServer.expect(requestTo("http://bancamiga.test/public/protected/pm/find"))
                .andRespond(withStatus(HttpStatusCode.valueOf(503))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"code\":503,\"mensaje\":\"Credenciales invalidas\"}"));

        assertThatThrownBy(() -> client.findPaymentMobile("584240000000", "0172", LocalDate.of(2026, 5, 6)))
                .isInstanceOf(BancamigaUnavailableException.class);
    }

    @Test
    void findPaymentMobile_throwsValidation_whenBankCodeIsNull() {
        assertThatThrownBy(() -> client.findPaymentMobile("584240000000", null, LocalDate.of(2026, 5, 6)))
                .isInstanceOf(BancamigaValidationException.class);
    }
}
