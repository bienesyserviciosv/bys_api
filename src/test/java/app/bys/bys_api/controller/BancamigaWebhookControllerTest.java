package app.bys.bys_api.controller;

import app.bys.bys_api.service.bancamiga.BancamigaVerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * MockMvc standalone (sin contexto Spring completo, sin filtro de seguridad de la app, sin DB) --
 * cubre la autenticacion propia del webhook (secreto Bearer) y la delegacion a
 * BancamigaVerificationService. Este endpoint es exactamente donde SI podemos probar de punta a
 * punta sin el host de Bancamiga: nosotros somos el receptor.
 */
@ExtendWith(MockitoExtension.class)
class BancamigaWebhookControllerTest {

    private static final String SECRET = "test-webhook-secret";
    private static final String PAYLOAD = """
            {
              "BancoOrig": "0172",
              "FechaMovimiento": "2026-05-06",
              "HoraMovimiento": "16:02:49",
              "NroReferencia": "018219",
              "PhoneOrig": "584240000000",
              "PhoneDest": "584240000000",
              "Status": "000",
              "Descripcion": "pago",
              "Amount": "5.64",
              "Refpk": "202605060172584240000000018219"
            }
            """;

    @Mock
    private BancamigaVerificationService bancamigaVerificationService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        BancamigaWebhookController controller = new BancamigaWebhookController(bancamigaVerificationService);
        ReflectionTestUtils.setField(controller, "webhookSecret", SECRET);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void receiveWebhook_returns200AndDelegates_whenSecretIsValid() throws Exception {
        mockMvc.perform(post("/bancamiga/webhook")
                        .header("Authorization", "Bearer " + SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Code").value(200))
                .andExpect(jsonPath("$.Refpk").value("202605060172584240000000018219"));

        verify(bancamigaVerificationService).handleWebhookMovement(any());
    }

    @Test
    void receiveWebhook_returns401_whenSecretIsWrong() throws Exception {
        mockMvc.perform(post("/bancamiga/webhook")
                        .header("Authorization", "Bearer wrong-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bancamigaVerificationService);
    }

    @Test
    void receiveWebhook_returns401_whenAuthorizationHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/bancamiga/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(bancamigaVerificationService);
    }

    @Test
    void receiveWebhook_returns401_whenWebhookSecretNotConfigured() throws Exception {
        BancamigaWebhookController controller = new BancamigaWebhookController(bancamigaVerificationService);
        ReflectionTestUtils.setField(controller, "webhookSecret", "");
        MockMvc mockMvcNoSecret = MockMvcBuilders.standaloneSetup(controller).build();

        mockMvcNoSecret.perform(post("/bancamiga/webhook")
                        .header("Authorization", "Bearer " + SECRET)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PAYLOAD))
                .andExpect(status().isUnauthorized());
    }
}
