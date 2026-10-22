package app.bys.bys_api.service.c2p;

import app.bys.bys_api.error.BancamigaTimeoutException;
import app.bys.bys_api.error.ConflictException;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pBanksResponse;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pPaymentData;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pPaymentRequest;
import app.bys.bys_api.model.dto.bancamiga.BancamigaC2pPaymentResponse;
import app.bys.bys_api.model.dto.c2p.C2pAttemptAdminDto;
import app.bys.bys_api.model.dto.c2p.C2pBankDto;
import app.bys.bys_api.model.dto.c2p.C2pPaymentRequestDto;
import app.bys.bys_api.model.dto.c2p.C2pPaymentResultDto;
import app.bys.bys_api.model.dto.c2p.C2pQuoteDto;
import app.bys.bys_api.model.entity.C2pAttempt;
import app.bys.bys_api.model.entity.ExchangeRate;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.enums.BankName;
import app.bys.bys_api.model.enums.C2pAttemptStatus;
import app.bys.bys_api.model.enums.PaymentStatus;
import app.bys.bys_api.model.enums.PaymentType;
import app.bys.bys_api.model.enums.RequestStatus;
import app.bys.bys_api.repository.C2pAttemptRepository;
import app.bys.bys_api.repository.OfferRepository;
import app.bys.bys_api.repository.PaymentRepository;
import app.bys.bys_api.service.NotificationService;
import app.bys.bys_api.service.PaymentService;
import app.bys.bys_api.service.exchange.ExchangeRateService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cobro en linea por C2P (pago movil con clave OTP) a traves de Bancamiga.
 *
 * Reglas importantes (ver doc "Doc API Bancamiga - PMC2P"):
 * - Cada intento fallido desde un banco tercero le cuesta una comision a BYS: se limitan los intentos
 *   fallidos por oferta (bancamiga.c2p.max-failed-attempts).
 * - Nunca se reintenta automaticamente un cobro sin respuesta (podria cobrarse dos veces): queda UNKNOWN y
 *   lo resuelve la conciliacion (C2pReconciliationScheduler) con c2p/mov.
 * - Solo puede haber un intento activo por oferta (indice unico parcial en la base de datos).
 * - La OTP nunca se guarda ni se registra en logs.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class C2pPaymentService {

    public static final String RESULT_APPROVED = "APPROVED";
    public static final String RESULT_REJECTED = "REJECTED";
    public static final String RESULT_PENDING = "PENDING_CONFIRMATION";
    public static final String RESULT_ERROR = "ERROR";

    static final Set<C2pAttemptStatus> ACTIVE_STATUSES = Set.of(
            C2pAttemptStatus.PROCESSING, C2pAttemptStatus.UNKNOWN, C2pAttemptStatus.APPROVED, C2pAttemptStatus.REVIEW);

    private static final String BANCAMIGA_BANK_CODE = "0172";
    private static final double AMOUNT_TOLERANCE = 0.01;
    private static final Duration BANKS_CACHE_TTL = Duration.ofHours(6);

    // Mensajes para el cliente segun el codigo de error de Bancamiga. Los no listados usan el texto del banco.
    private static final Map<String, String> ERROR_MESSAGES = Map.of(
            "E029", "La clave de pago (OTP) vencio. Genera una nueva en tu banco e intentalo de nuevo."
    );

    private final C2pAttemptRepository attemptRepository;
    private final OfferRepository offerRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final BancamigaC2pClient c2pClient;
    private final ExchangeRateService exchangeRateService;
    private final TransactionTemplate transactionTemplate;

    @Value("${bancamiga.enabled}")
    private boolean bancamigaEnabled;

    @Value("${bancamiga.c2p.enabled}")
    private boolean c2pEnabled;

    @Value("${bancamiga.c2p.max-failed-attempts}")
    private int maxFailedAttempts;

    @Value("${bancamiga.c2p.third-party-max-amount}")
    private BigDecimal thirdPartyMaxAmount;

    @Value("${bancamiga.c2p.motivo-prefix}")
    private String motivoPrefix;

    private volatile List<C2pBankDto> cachedBanks;
    private volatile LocalDateTime cachedBanksAt;

    public boolean isEnabled() {
        return bancamigaEnabled && c2pEnabled;
    }

    // ------------------------------------------------------------------ bancos

    public List<C2pBankDto> getBanks() {
        ensureEnabled();
        if (cachedBanks != null && cachedBanksAt != null
                && cachedBanksAt.isAfter(LocalDateTime.now().minus(BANKS_CACHE_TTL))) {
            return cachedBanks;
        }
        BancamigaC2pBanksResponse response = c2pClient.listBanks();
        List<C2pBankDto> banks = response == null || response.getData() == null ? List.of()
                : response.getData().stream()
                .filter(b -> b.getCodBanco() != null)
                .map(b -> new C2pBankDto(b.getCodBanco(), b.getNombreBanco()))
                .toList();
        if (!banks.isEmpty()) {
            cachedBanks = banks;
            cachedBanksAt = LocalDateTime.now();
        }
        return banks;
    }

    // ------------------------------------------------------------------ cotizacion

    public C2pQuoteDto quote(Long offerId, String authenticatedEmail) {
        ExchangeRate rate = exchangeRateService.getCurrentOrThrow();
        return transactionTemplate.execute(status -> {
            Offer offer = findOffer(offerId);
            checkOwner(offer, authenticatedEmail);

            BigDecimal amountUsd = usdAmount(offer);
            BigDecimal amountBs = ExchangeRateService.toBolivars(amountUsd, rate.getRate());
            int failed = (int) attemptRepository.countByOfferIdAndStatus(offer.getId(), C2pAttemptStatus.REJECTED);
            int remaining = Math.max(0, maxFailedAttempts - failed);

            String reason = null;
            if (!isEnabled()) {
                reason = "El pago en linea no esta disponible en este momento.";
            } else if (payabilityProblem(offer) != null) {
                reason = payabilityProblem(offer);
            } else if (remaining == 0) {
                reason = "Se alcanzo el limite de intentos de pago en linea para esta oferta. Usa otro metodo de pago o escribe a soporte.";
            } else if (attemptRepository.existsByOfferIdAndStatusIn(offer.getId(), ACTIVE_STATUSES)) {
                reason = "Ya hay un pago en linea en proceso para esta oferta.";
            }

            return C2pQuoteDto.builder()
                    .offerId(offer.getId())
                    .amountUsd(amountUsd)
                    .exchangeRate(rate.getRate())
                    .exchangeRateDate(rate.getFetchedAt())
                    .amountBs(amountBs)
                    .maxAttempts(maxFailedAttempts)
                    .attemptsRemaining(remaining)
                    .available(reason == null)
                    .unavailableReason(reason)
                    .build();
        });
    }

    // ------------------------------------------------------------------ cobro

    public C2pPaymentResultDto pay(C2pPaymentRequestDto dto, String authenticatedEmail) {
        ensureEnabled();
        validateBank(dto.getBankCode());
        ExchangeRate rate = exchangeRateService.getCurrentOrThrow();

        String phone = toBancamigaPhone(dto.getPhoneCode().getCode(), dto.getPhoneNumber());
        String idDocument = dto.getIdType().toUpperCase() + dto.getIdNumber();

        // 1) Validaciones y registro del intento (transaccion corta, antes de llamar al banco)
        PreparedAttempt prepared;
        try {
            prepared = transactionTemplate.execute(status -> {
                Offer offer = findOffer(dto.getOfferId());
                checkOwner(offer, authenticatedEmail);
                String problem = payabilityProblem(offer);
                if (problem != null) {
                    throw new ConflictException(problem);
                }
                long failed = attemptRepository.countByOfferIdAndStatus(offer.getId(), C2pAttemptStatus.REJECTED);
                if (failed >= maxFailedAttempts) {
                    throw new ConflictException("Se alcanzo el limite de intentos de pago en linea para esta oferta. "
                            + "Usa otro metodo de pago o escribe a soporte.");
                }
                if (attemptRepository.existsByOfferIdAndStatusIn(offer.getId(), ACTIVE_STATUSES)) {
                    throw new ConflictException("Ya hay un pago en linea en proceso para esta oferta.");
                }

                BigDecimal amountUsd = usdAmount(offer);
                BigDecimal amountBs = ExchangeRateService.toBolivars(amountUsd, rate.getRate());
                if (dto.getExpectedAmountBs() != null
                        && dto.getExpectedAmountBs().setScale(2, RoundingMode.HALF_UP).compareTo(amountBs) != 0) {
                    throw new ConflictException("La tasa de cambio se actualizo. El nuevo monto es Bs " + amountBs.toPlainString()
                            + ". Revisa el monto y confirma de nuevo.");
                }
                if (!BANCAMIGA_BANK_CODE.equals(dto.getBankCode()) && amountBs.compareTo(thirdPartyMaxAmount) > 0) {
                    throw new ConflictException("El monto supera el limite permitido por pago movil desde otros bancos.");
                }

                LocalDateTime now = LocalDateTime.now();
                C2pAttempt attempt = attemptRepository.saveAndFlush(C2pAttempt.builder()
                        .offer(offer)
                        .finalUser(offer.getServiceRequest().getFinalUser())
                        .bankCode(dto.getBankCode())
                        .phoneCode(dto.getPhoneCode())
                        .phoneNumber(dto.getPhoneNumber())
                        .phone(phone)
                        .idDocument(idDocument)
                        .amountUsd(amountUsd)
                        .exchangeRate(rate.getRate())
                        .amountBs(amountBs)
                        .status(C2pAttemptStatus.PROCESSING)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
                return new PreparedAttempt(attempt.getId(), offer.getId(), amountBs, (int) failed);
            });
        } catch (DataIntegrityViolationException e) {
            // Dos toques seguidos: el indice unico parcial impide un segundo intento activo para la misma oferta
            throw new ConflictException("Ya hay un pago en linea en proceso para esta oferta.");
        }

        // 2) Cobro en Bancamiga (fuera de la transaccion)
        BancamigaC2pPaymentRequest request = BancamigaC2pPaymentRequest.builder()
                .bancoOrigen(dto.getBankCode())
                .telfOrigen(phone)
                .dniOrigen(idDocument)
                .monto(toBancamigaAmount(prepared.amountBs()))
                .motivo(motivoPrefix + " oferta " + prepared.offerId())
                .otp(dto.getOtp())
                .build();

        BancamigaC2pPaymentResponse response;
        try {
            response = c2pClient.pay(request);
        } catch (BancamigaTimeoutException e) {
            log.warn("Cobro C2P {} sin respuesta de Bancamiga, queda UNKNOWN para conciliar: {}", prepared.attemptId(), e.getMessage());
            updateStatus(prepared.attemptId(), C2pAttemptStatus.UNKNOWN, null, e.getMessage());
            return pendingResult(prepared);
        } catch (RuntimeException e) {
            log.warn("Cobro C2P {} no se pudo procesar: {}", prepared.attemptId(), e.getMessage());
            updateStatus(prepared.attemptId(), C2pAttemptStatus.ERROR, null, e.getMessage());
            return C2pPaymentResultDto.builder()
                    .result(RESULT_ERROR)
                    .message("No pudimos procesar el pago en este momento y no se realizo ningun cobro. Intenta de nuevo en unos minutos.")
                    .attemptId(prepared.attemptId())
                    .amountBs(prepared.amountBs())
                    .attemptsRemaining(maxFailedAttempts - prepared.failedBefore())
                    .build();
        }

        BancamigaC2pPaymentData data = response != null ? response.getData() : null;
        if (data == null || data.getStatus() == null) {
            log.warn("Respuesta de Bancamiga sin datos para el cobro C2P {}, queda UNKNOWN para conciliar", prepared.attemptId());
            updateStatus(prepared.attemptId(), C2pAttemptStatus.UNKNOWN, null, "Respuesta sin datos");
            return pendingResult(prepared);
        }

        if (isApproved(data.getStatus())) {
            String note = null;
            if (data.getAmount() != null && Math.abs(data.getAmount() - prepared.amountBs().doubleValue()) > AMOUNT_TOLERANCE) {
                note = "Monto aprobado por el banco (" + data.getAmount() + ") distinto al esperado (" + prepared.amountBs() + ")";
                log.error("Cobro C2P {}: {}", prepared.attemptId(), note);
            }
            Long paymentId = approve(prepared.attemptId(), data.getNroref(), data.getDataorig(), data.getId(), "C2P", note);
            if (paymentId == null) {
                return C2pPaymentResultDto.builder()
                        .result(RESULT_PENDING)
                        .message("Tu pago fue recibido y lo esta revisando nuestro equipo. Te avisaremos en breve.")
                        .attemptId(prepared.attemptId())
                        .reference(data.getNroref())
                        .amountBs(prepared.amountBs())
                        .build();
            }
            return C2pPaymentResultDto.builder()
                    .result(RESULT_APPROVED)
                    .message("Pago aprobado. Referencia " + data.getNroref() + ".")
                    .attemptId(prepared.attemptId())
                    .paymentId(paymentId)
                    .reference(data.getNroref())
                    .amountBs(prepared.amountBs())
                    .build();
        }

        // NO APROBADO
        updateRejected(prepared.attemptId(), data);
        int remaining = Math.max(0, maxFailedAttempts - (prepared.failedBefore() + 1));
        String message = friendlyError(data.getCode(), data.getError());
        message += remaining > 0
                ? " Te quedan " + remaining + " intento(s)."
                : " Se alcanzo el limite de intentos de pago en linea; usa otro metodo de pago o escribe a soporte.";
        return C2pPaymentResultDto.builder()
                .result(RESULT_REJECTED)
                .message(message)
                .attemptId(prepared.attemptId())
                .amountBs(prepared.amountBs())
                .attemptsRemaining(remaining)
                .build();
    }

    /** Estado de un intento, para que la app consulte cuando el resultado quedo pendiente. */
    public C2pPaymentResultDto getAttemptResult(Long attemptId, String authenticatedEmail) {
        return transactionTemplate.execute(status -> {
            C2pAttempt attempt = attemptRepository.findById(attemptId)
                    .orElseThrow(() -> new EntityNotFoundException("C2P attempt with id: " + attemptId + " not found"));
            if (!authenticatedEmail.equals(attempt.getFinalUser().getEmail())) {
                throw new ConflictException("You cannot see a payment attempt of another user");
            }
            int failed = (int) attemptRepository.countByOfferIdAndStatus(attempt.getOffer().getId(), C2pAttemptStatus.REJECTED);
            int remaining = Math.max(0, maxFailedAttempts - failed);
            C2pPaymentResultDto.C2pPaymentResultDtoBuilder builder = C2pPaymentResultDto.builder()
                    .attemptId(attempt.getId())
                    .amountBs(attempt.getAmountBs())
                    .reference(attempt.getBancamigaNroref())
                    .attemptsRemaining(remaining);
            return switch (attempt.getStatus()) {
                case APPROVED -> builder.result(RESULT_APPROVED)
                        .paymentId(attempt.getPayment() != null ? attempt.getPayment().getId() : null)
                        .message("Pago aprobado. Referencia " + attempt.getBancamigaNroref() + ".")
                        .build();
                case REJECTED -> builder.result(RESULT_REJECTED)
                        .message(friendlyError(attempt.getErrorCode(), attempt.getErrorMessage()))
                        .build();
                case ERROR -> builder.result(RESULT_ERROR)
                        .message("No se realizo ningun cobro. Puedes intentar de nuevo.")
                        .build();
                case PROCESSING, UNKNOWN, REVIEW -> builder.result(RESULT_PENDING)
                        .message("Estamos confirmando tu pago con el banco. Te avisaremos en cuanto este listo.")
                        .build();
            };
        });
    }

    public Page<C2pAttemptAdminDto> listAttempts(C2pAttemptStatus status, Pageable pageable) {
        return transactionTemplate.execute(tx -> attemptRepository.findAllByStatus(status, pageable).map(a -> {
            ServiceRequest request = a.getOffer().getServiceRequest();
            FinalUser user = a.getFinalUser();
            return C2pAttemptAdminDto.builder()
                    .id(a.getId())
                    .status(a.getStatus())
                    .offerId(a.getOffer().getId())
                    .requestId(request != null ? request.getId() : null)
                    .userId(user.getId())
                    .userName(user.getName())
                    .paymentId(a.getPayment() != null ? a.getPayment().getId() : null)
                    .bankCode(a.getBankCode())
                    .phone(a.getPhone())
                    .idDocument(a.getIdDocument())
                    .amountUsd(a.getAmountUsd())
                    .exchangeRate(a.getExchangeRate())
                    .amountBs(a.getAmountBs())
                    .reference(a.getBancamigaNroref())
                    .errorCode(a.getErrorCode())
                    .errorMessage(a.getErrorMessage())
                    .createdAt(a.getCreatedAt())
                    .updatedAt(a.getUpdatedAt())
                    .build();
        }));
    }

    // ------------------------------------------------------------------ aprobacion (tambien usada por la conciliacion)

    /**
     * Registra el pago aprobado por el banco y acepta la oferta (misma logica que un pago verificado).
     * Devuelve el id del pago, o null si la oferta ya no admitia el pago (el intento queda REVIEW para
     * que un administrador lo revise y, si corresponde, reembolse).
     */
    public Long approve(Long attemptId, String nroref, String dataorig, String bancamigaId, String matchSource, String note) {
        ApprovalResult result = transactionTemplate.execute(status -> {
            C2pAttempt attempt = attemptRepository.findById(attemptId)
                    .orElseThrow(() -> new EntityNotFoundException("C2P attempt with id: " + attemptId + " not found"));
            if (attempt.getPayment() != null) {
                return new ApprovalResult(attempt.getPayment().getId(), null, null, null, null);
            }
            LocalDateTime now = LocalDateTime.now();
            attempt.setBancamigaNroref(nroref);
            attempt.setBancamigaDataorig(dataorig);
            attempt.setBancamigaId(bancamigaId);
            attempt.setErrorCode(null);
            attempt.setErrorMessage(note);
            attempt.setUpdatedAt(now);

            Offer offer = attempt.getOffer();
            String problem = payabilityProblem(offer);
            if (problem != null) {
                attempt.setStatus(C2pAttemptStatus.REVIEW);
                attempt.setErrorMessage("Cobro APROBADO por el banco (ref " + nroref + ") pero la oferta ya no admitia el pago: "
                        + problem + " Revisar y reembolsar si corresponde.");
                attemptRepository.save(attempt);
                log.error("Cobro C2P {} aprobado pero no se pudo asociar a la oferta {}: {}", attemptId, offer.getId(), problem);
                return null;
            }

            ServiceRequest request = offer.getServiceRequest();
            Payment payment = paymentRepository.save(Payment.builder()
                    .bank(BankName.fromBancamigaCode(attempt.getBankCode()))
                    .paymentType(PaymentType.C2P)
                    .paymentStatus(PaymentStatus.PENDING)
                    .referenceNumber(nroref)
                    .paymentDate(now)
                    .phoneCode(attempt.getPhoneCode())
                    .phoneNumber(attempt.getPhoneNumber())
                    .idNumber(attempt.getIdDocument())
                    .amountInBolivars(attempt.getAmountBs().doubleValue())
                    .bancamigaRefpk(bancamigaId)
                    .bancamigaVerifiedAt(now)
                    .bancamigaMatchSource(matchSource)
                    .finalUser(request.getFinalUser())
                    .serviceProvider(offer.getProvider())
                    .offer(offer)
                    .build());
            offer.setPayment(payment);

            attempt.setPayment(payment);
            attempt.setStatus(C2pAttemptStatus.APPROVED);
            attemptRepository.save(attempt);

            paymentService.acceptPayment(payment.getId());

            return new ApprovalResult(payment.getId(), request.getFinalUser().getId(), offer.getProvider().getId(),
                    request.getId(), offer.getId());
        });

        if (result == null) {
            return null;
        }
        if (result.userId() != null) {
            try {
                notificationService.notifyPaymentAccepted(result.userId(), result.providerId(), result.requestId(), result.offerId());
                notificationService.notifyAdminOfNewPayment(result.userId(), result.providerId(), result.requestId(), PaymentType.C2P);
            } catch (RuntimeException e) {
                log.warn("Pago C2P {} aprobado, pero fallo el envio de notificaciones: {}", result.paymentId(), e.getMessage());
            }
            log.info("Cobro C2P {} aprobado (ref {}, fuente {}), pago {}", attemptId, nroref, matchSource, result.paymentId());
        }
        return result.paymentId();
    }

    public void updateStatus(Long attemptId, C2pAttemptStatus newStatus, String errorCode, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> attemptRepository.findById(attemptId).ifPresent(attempt -> {
            attempt.setStatus(newStatus);
            attempt.setErrorCode(errorCode);
            attempt.setErrorMessage(truncate(errorMessage));
            attempt.setUpdatedAt(LocalDateTime.now());
            attemptRepository.save(attempt);
        }));
    }

    private void updateRejected(Long attemptId, BancamigaC2pPaymentData data) {
        transactionTemplate.executeWithoutResult(status -> attemptRepository.findById(attemptId).ifPresent(attempt -> {
            attempt.setStatus(C2pAttemptStatus.REJECTED);
            attempt.setErrorCode(data.getCode());
            attempt.setErrorMessage(truncate(data.getError()));
            attempt.setBancamigaId(data.getId());
            attempt.setUpdatedAt(LocalDateTime.now());
            attemptRepository.save(attempt);
        }));
    }

    // ------------------------------------------------------------------ utilidades

    static boolean isApproved(String bancamigaStatus) {
        return bancamigaStatus != null && "APROBADO".equalsIgnoreCase(bancamigaStatus.trim());
    }

    /** "58" + operadora sin el 0 inicial + numero. Ej. 0414 + 1234567 -> 584141234567 */
    static String toBancamigaPhone(String phoneCode, String phoneNumber) {
        return "58" + phoneCode.substring(1) + phoneNumber;
    }

    /** Monto con 2 decimales y coma decimal, sin separador de miles. Ej. 1234.5 -> "1234,50" */
    static String toBancamigaAmount(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString().replace('.', ',');
    }

    private String friendlyError(String code, String bankMessage) {
        if (code != null && ERROR_MESSAGES.containsKey(code.trim())) {
            return ERROR_MESSAGES.get(code.trim());
        }
        if (bankMessage != null && !bankMessage.isBlank()) {
            return "El banco no aprobo el pago: " + bankMessage.trim() + ".";
        }
        return "El banco no aprobo el pago.";
    }

    private void ensureEnabled() {
        if (!isEnabled()) {
            throw new ConflictException("El pago en linea no esta disponible en este momento.");
        }
    }

    private void validateBank(String bankCode) {
        try {
            List<C2pBankDto> banks = getBanks();
            if (!banks.isEmpty() && banks.stream().noneMatch(b -> bankCode.equals(b.getCode()))) {
                throw new ConflictException("El banco seleccionado no acepta pagos C2P.");
            }
        } catch (ConflictException e) {
            throw e;
        } catch (RuntimeException e) {
            // Si no se pudo consultar la lista, se deja que Bancamiga valide el banco
            log.warn("No se pudo consultar la lista de bancos C2P: {}", e.getMessage());
        }
    }

    private Offer findOffer(Long offerId) {
        return offerRepository.findById(offerId)
                .orElseThrow(() -> new EntityNotFoundException("Offer with id: " + offerId + " not found"));
    }

    private void checkOwner(Offer offer, String authenticatedEmail) {
        FinalUser owner = offer.getServiceRequest().getFinalUser();
        if (owner == null || !authenticatedEmail.equals(owner.getEmail())) {
            throw new ConflictException("You cannot create a payment for another user's request");
        }
    }

    /** null si la oferta se puede pagar; si no, el motivo para el cliente. */
    private String payabilityProblem(Offer offer) {
        ServiceRequest request = offer.getServiceRequest();
        if (request.getAcceptedOffer() == null || !offer.getId().equals(request.getAcceptedOffer().getId())) {
            return "La oferta no ha sido aceptada.";
        }
        if (offer.getPayment() != null) {
            return "Esta oferta ya tiene un pago registrado. Si necesitas ayuda, escribe a soporte.";
        }
        RequestStatus requestStatus = request.getRequestStatus();
        if (requestStatus == RequestStatus.ACCEPTED || requestStatus == RequestStatus.COMPLETED
                || requestStatus == RequestStatus.CANCELLED) {
            return "La solicitud ya no admite pagos.";
        }
        return null;
    }

    private BigDecimal usdAmount(Offer offer) {
        return BigDecimal.valueOf(offer.getPrice()).setScale(2, RoundingMode.HALF_UP);
    }

    private C2pPaymentResultDto pendingResult(PreparedAttempt prepared) {
        return C2pPaymentResultDto.builder()
                .result(RESULT_PENDING)
                .message("Estamos confirmando tu pago con el banco. No intentes pagar de nuevo; te avisaremos en unos minutos.")
                .attemptId(prepared.attemptId())
                .amountBs(prepared.amountBs())
                .build();
    }

    private static String truncate(String text) {
        if (text == null) {
            return null;
        }
        return text.length() > 1000 ? text.substring(0, 1000) : text;
    }

    private record PreparedAttempt(Long attemptId, Long offerId, BigDecimal amountBs, int failedBefore) {
    }

    private record ApprovalResult(Long paymentId, Long userId, Long providerId, Long requestId, Long offerId) {
    }
}
