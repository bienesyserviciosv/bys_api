package app.bys.bys_api.model.dto.c2p;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class C2pPaymentResultDto {

    /**
     * APPROVED: pago aprobado, la oferta quedo pagada.
     * REJECTED: el banco rechazo el pago (no se cobro). Ver message y attempts_remaining.
     * PENDING_CONFIRMATION: no hubo respuesta del banco; se confirmara en unos minutos (consultar /attempt/{id}).
     * ERROR: no se pudo procesar (no se cobro); se puede intentar de nuevo.
     */
    @JsonProperty("result")
    private String result;

    @JsonProperty("message")
    private String message;

    @JsonProperty("attempt_id")
    private Long attemptId;

    @JsonProperty("payment_id")
    private Long paymentId;

    @JsonProperty("reference")
    private String reference;

    @JsonProperty("amount_bs")
    private BigDecimal amountBs;

    @JsonProperty("attempts_remaining")
    private Integer attemptsRemaining;
}
