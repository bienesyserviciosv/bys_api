package app.bys.bys_api.service.bancamiga;

import app.bys.bys_api.error.BancamigaUnavailableException;
import app.bys.bys_api.error.ServiceRequestAlreadyAcceptedException;
import app.bys.bys_api.model.dto.bancamiga.BancamigaFindPaymentResponse;
import app.bys.bys_api.model.dto.bancamiga.BancamigaMovementDto;
import app.bys.bys_api.model.dto.bancamiga.BancamigaWebhookPayload;
import app.bys.bys_api.model.entity.*;
import app.bys.bys_api.model.enums.BankName;
import app.bys.bys_api.model.enums.PaymentStatus;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.model.enums.PhoneCode;
import app.bys.bys_api.repository.PaymentRepository;
import app.bys.bys_api.service.NotificationService;
import app.bys.bys_api.service.PaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Cubre el matching y la auto-aceptacion de BancamigaVerificationService sin tocar el host real de
 * Bancamiga: BancamigaClient/PaymentRepository/PaymentService/NotificationService estan mockeados.
 */
@ExtendWith(MockitoExtension.class)
class BancamigaVerificationServiceTest {

    private static final String EXPECTED_PHONE = "584140000000";
    private static final String EXPECTED_BANK_CODE = "0172";

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BancamigaClient bancamigaClient;
    @Mock
    private PaymentService paymentService;
    @Mock
    private NotificationService notificationService;

    private BancamigaVerificationService verificationService;

    @BeforeEach
    void setUp() {
        verificationService = new BancamigaVerificationService(paymentRepository, bancamigaClient, paymentService, notificationService);
        ReflectionTestUtils.setField(verificationService, "bancamigaEnabled", true);
        ReflectionTestUtils.setField(verificationService, "receivingPhone", "584120000000");
    }

    private Payment pendingMobilePayment(Long id) {
        ServiceRequest request = ServiceRequest.builder().id(20L).build();
        Offer offer = Offer.builder().id(10L).serviceRequest(request).build();
        FinalUser finalUser = FinalUser.builder().id(30L).build();
        ServiceProvider provider = ServiceProvider.builder().id(40L).build();

        return Payment.builder()
                .id(id)
                .paymentType(PaymentType.MOBILE)
                .paymentStatus(PaymentStatus.PENDING)
                .amountInBolivars(100.0)
                .bank(BankName.BANK_BANCAMIGA)
                .phoneCode(PhoneCode.CODE_0414)
                .phoneNumber("0000000")
                .paymentDate(LocalDateTime.of(2026, 5, 6, 10, 0))
                .referenceNumber("575202")
                .offer(offer)
                .finalUser(finalUser)
                .serviceProvider(provider)
                .build();
    }

    private BancamigaMovementDto matchingMovement() {
        BancamigaMovementDto movement = new BancamigaMovementDto();
        movement.setAmount(100.0);
        movement.setPhoneOrig(EXPECTED_PHONE);
        movement.setPhoneDest("584120000000");
        movement.setBancoOrig(EXPECTED_BANK_CODE);
        movement.setFechaMovimiento("2026-05-06");
        movement.setNroReferenciaCorto("575202");
        movement.setRefpk("REFPK-1");
        return movement;
    }

    @Test
    void verifyAndMaybeAccept_doesNothing_whenBancamigaDisabled() {
        ReflectionTestUtils.setField(verificationService, "bancamigaEnabled", false);

        verificationService.verifyAndMaybeAccept(1L);

        verifyNoInteractions(paymentRepository, bancamigaClient, paymentService, notificationService);
    }

    @Test
    void verifyAndMaybeAccept_acceptsPayment_whenMovementMatchesExactly() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        BancamigaFindPaymentResponse response = new BancamigaFindPaymentResponse();
        response.setLista(List.of(matchingMovement()));
        when(bancamigaClient.findPaymentMobile(eq(EXPECTED_PHONE), eq(EXPECTED_BANK_CODE), any())).thenReturn(response);

        Payment accepted = pendingMobilePayment(1L);
        accepted.setPaymentStatus(PaymentStatus.ACCEPTED);
        when(paymentService.acceptPayment(1L)).thenReturn(accepted);

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentService).acceptPayment(1L);
        verify(paymentRepository).save(argThat(p ->
                "REFPK-1".equals(p.getBancamigaRefpk())
                        && "SYNC_CHECK".equals(p.getBancamigaMatchSource())
                        && p.getBancamigaVerifiedAt() != null));
        verify(notificationService).notifyPaymentAccepted(30L, 40L, 20L, 10L);
    }

    @Test
    void verifyAndMaybeAccept_amountToleranceAllowsSmallRoundingDifference() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        BancamigaMovementDto movement = matchingMovement();
        movement.setAmount(100.005);
        BancamigaFindPaymentResponse response = new BancamigaFindPaymentResponse();
        response.setLista(List.of(movement));
        when(bancamigaClient.findPaymentMobile(any(), any(), any())).thenReturn(response);
        when(paymentService.acceptPayment(1L)).thenReturn(payment);

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentService).acceptPayment(1L);
    }

    @Test
    void verifyAndMaybeAccept_leavesPending_whenAmountDoesNotMatch() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        BancamigaMovementDto movement = matchingMovement();
        movement.setAmount(999.0);
        BancamigaFindPaymentResponse response = new BancamigaFindPaymentResponse();
        response.setLista(List.of(movement));
        when(bancamigaClient.findPaymentMobile(any(), any(), any())).thenReturn(response);

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentService, never()).acceptPayment(any());
    }

    @Test
    void verifyAndMaybeAccept_leavesPending_whenPhoneDoesNotMatch() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        BancamigaMovementDto movement = matchingMovement();
        movement.setPhoneOrig("584160000000");
        BancamigaFindPaymentResponse response = new BancamigaFindPaymentResponse();
        response.setLista(List.of(movement));
        when(bancamigaClient.findPaymentMobile(any(), any(), any())).thenReturn(response);

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentService, never()).acceptPayment(any());
    }

    @Test
    void verifyAndMaybeAccept_leavesPending_whenBankDoesNotMatch() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        BancamigaMovementDto movement = matchingMovement();
        movement.setBancoOrig("0105");
        BancamigaFindPaymentResponse response = new BancamigaFindPaymentResponse();
        response.setLista(List.of(movement));
        when(bancamigaClient.findPaymentMobile(any(), any(), any())).thenReturn(response);

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentService, never()).acceptPayment(any());
    }

    @Test
    void verifyAndMaybeAccept_leavesPending_whenDateDoesNotMatch() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        BancamigaMovementDto movement = matchingMovement();
        movement.setFechaMovimiento("2026-05-07");
        BancamigaFindPaymentResponse response = new BancamigaFindPaymentResponse();
        response.setLista(List.of(movement));
        when(bancamigaClient.findPaymentMobile(any(), any(), any())).thenReturn(response);

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentService, never()).acceptPayment(any());
    }

    @Test
    void verifyAndMaybeAccept_leavesPending_whenNoMovementsReturned() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        BancamigaFindPaymentResponse response = new BancamigaFindPaymentResponse();
        response.setLista(List.of());
        when(bancamigaClient.findPaymentMobile(any(), any(), any())).thenReturn(response);

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentService, never()).acceptPayment(any());
    }

    @Test
    void verifyAndMaybeAccept_leavesPending_whenBancamigaThrowsUnavailable() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));
        when(bancamigaClient.findPaymentMobile(any(), any(), any()))
                .thenThrow(new BancamigaUnavailableException("timeout hablando con Bancamiga"));

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentService, never()).acceptPayment(any());
    }

    @Test
    void verifyAndMaybeAccept_skipsBankLookup_whenBankHasNoBancamigaCode() {
        Payment payment = pendingMobilePayment(1L);
        payment.setBank(BankName.BANK_BANESCO); // no tiene bancamigaCode mapeado
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        verificationService.verifyAndMaybeAccept(1L);

        verifyNoInteractions(bancamigaClient);
        verify(paymentService, never()).acceptPayment(any());
    }

    @Test
    void verifyAndMaybeAccept_ignoresNonPendingPayment() {
        Payment payment = pendingMobilePayment(1L);
        payment.setPaymentStatus(PaymentStatus.ACCEPTED);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        verificationService.verifyAndMaybeAccept(1L);

        verifyNoInteractions(bancamigaClient);
    }

    @Test
    void acceptFromMovement_swallowsRaceWithServiceRequestAlreadyAccepted() {
        Payment payment = pendingMobilePayment(1L);
        when(paymentRepository.findById(1L)).thenReturn(Optional.of(payment));

        BancamigaFindPaymentResponse response = new BancamigaFindPaymentResponse();
        response.setLista(List.of(matchingMovement()));
        when(bancamigaClient.findPaymentMobile(any(), any(), any())).thenReturn(response);
        when(paymentService.acceptPayment(1L)).thenThrow(new ServiceRequestAlreadyAcceptedException("ya aceptada"));

        verificationService.verifyAndMaybeAccept(1L);

        verify(paymentRepository, never()).save(any());
        verifyNoInteractions(notificationService);
    }

    @Test
    void handleWebhookMovement_acceptsMatchingPendingPayment() {
        Payment payment = pendingMobilePayment(2L);
        when(paymentRepository.findByPaymentTypeAndPaymentStatusAndPaymentDateAfter(
                eq(PaymentType.MOBILE), eq(PaymentStatus.PENDING), any()))
                .thenReturn(List.of(payment));
        when(paymentRepository.findById(2L)).thenReturn(Optional.of(payment));

        Payment accepted = pendingMobilePayment(2L);
        accepted.setPaymentStatus(PaymentStatus.ACCEPTED);
        when(paymentService.acceptPayment(2L)).thenReturn(accepted);

        BancamigaWebhookPayload payload = new BancamigaWebhookPayload();
        payload.setAmount("100.0");
        payload.setPhoneOrig(EXPECTED_PHONE);
        payload.setPhoneDest("584120000000");
        payload.setBancoOrig(EXPECTED_BANK_CODE);
        payload.setFechaMovimiento("2026-05-06");
        payload.setNroReferencia("000000575202");
        payload.setRefpk("REFPK-WEBHOOK");

        verificationService.handleWebhookMovement(payload);

        verify(paymentService).acceptPayment(2L);
        verify(paymentRepository).save(argThat(p ->
                "REFPK-WEBHOOK".equals(p.getBancamigaRefpk()) && "WEBHOOK".equals(p.getBancamigaMatchSource())));
    }

    @Test
    void handleWebhookMovement_doesNothing_whenNoCandidateMatches() {
        when(paymentRepository.findByPaymentTypeAndPaymentStatusAndPaymentDateAfter(
                eq(PaymentType.MOBILE), eq(PaymentStatus.PENDING), any()))
                .thenReturn(List.of());

        BancamigaWebhookPayload payload = new BancamigaWebhookPayload();
        payload.setAmount("100.0");
        payload.setRefpk("REFPK-ORPHAN");

        verificationService.handleWebhookMovement(payload);

        verify(paymentService, never()).acceptPayment(any());
    }
}
