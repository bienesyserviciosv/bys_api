package app.bys.bys_api.controller.super_admin;

import app.bys.bys_api.model.dto.c2p.ExchangeRateDto;
import app.bys.bys_api.model.dto.c2p.ManualExchangeRateDto;
import app.bys.bys_api.model.entity.ExchangeRate;
import app.bys.bys_api.service.exchange.ExchangeRateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/super_admin/exchange_rate")
@PreAuthorize("hasAuthority('ROLE_SUPER_ADMIN')")
public class SuperAdminExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    /** Ultima tasa guardada (aunque este vencida), para revisar el estado. */
    @GetMapping
    public ResponseEntity<ExchangeRateDto> getLatest() {
        return ResponseEntity.ok(exchangeRateService.getLatest().map(this::toDto).orElse(null));
    }

    /** Fuerza la lectura de la tasa del BCV (o del respaldo). */
    @PostMapping("/refresh")
    public ResponseEntity<ExchangeRateDto> refresh() {
        exchangeRateService.refresh();
        return ResponseEntity.ok(exchangeRateService.getLatest().map(this::toDto).orElse(null));
    }

    /** Fija la tasa a mano (por ejemplo, si el BCV no responde). */
    @PostMapping
    public ResponseEntity<ExchangeRateDto> setManual(@Valid @RequestBody ManualExchangeRateDto dto, Authentication authentication) {
        return ResponseEntity.ok(toDto(exchangeRateService.setManualRate(dto.getRate(), authentication.getName())));
    }

    private ExchangeRateDto toDto(ExchangeRate rate) {
        return ExchangeRateDto.builder()
                .rate(rate.getRate())
                .source(rate.getSource())
                .fetchedAt(rate.getFetchedAt())
                .createdBy(rate.getCreatedBy())
                .build();
    }
}
