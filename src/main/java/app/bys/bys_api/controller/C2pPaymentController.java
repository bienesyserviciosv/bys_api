package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.c2p.C2pBankDto;
import app.bys.bys_api.model.dto.c2p.C2pPaymentRequestDto;
import app.bys.bys_api.model.dto.c2p.C2pPaymentResultDto;
import app.bys.bys_api.model.dto.c2p.C2pQuoteDto;
import app.bys.bys_api.service.c2p.C2pPaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pago en linea por C2P (pago movil con clave OTP).
 * Flujo de la app: GET /banks -> GET /quote/{offerId} -> POST (con la OTP) -> si queda PENDING_CONFIRMATION,
 * consultar GET /attempt/{id} cada cierto tiempo.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/payment/c2p")
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN', 'ROLE_USER')")
public class C2pPaymentController {

    private final C2pPaymentService c2pPaymentService;

    @GetMapping("/banks")
    public ResponseEntity<List<C2pBankDto>> getBanks() {
        return ResponseEntity.ok(c2pPaymentService.getBanks());
    }

    @GetMapping("/quote/{offerId}")
    public ResponseEntity<C2pQuoteDto> quote(@PathVariable Long offerId, Authentication authentication) {
        return ResponseEntity.ok(c2pPaymentService.quote(offerId, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<C2pPaymentResultDto> pay(@Valid @RequestBody C2pPaymentRequestDto request,
                                                   Authentication authentication) {
        return ResponseEntity.ok(c2pPaymentService.pay(request, authentication.getName()));
    }

    @GetMapping("/attempt/{attemptId}")
    public ResponseEntity<C2pPaymentResultDto> getAttempt(@PathVariable Long attemptId, Authentication authentication) {
        return ResponseEntity.ok(c2pPaymentService.getAttemptResult(attemptId, authentication.getName()));
    }
}
