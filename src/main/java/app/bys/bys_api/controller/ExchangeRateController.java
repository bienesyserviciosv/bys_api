package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.c2p.ExchangeRateDto;
import app.bys.bys_api.model.entity.ExchangeRate;
import app.bys.bys_api.service.exchange.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tasa vigente (Bs por 1 USD) para que la app muestre los montos en bolivares con la misma tasa del cobro. */
@RestController
@RequiredArgsConstructor
@RequestMapping("/exchange_rate")
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;

    @GetMapping
    public ResponseEntity<ExchangeRateDto> getCurrent() {
        ExchangeRate rate = exchangeRateService.getCurrentOrThrow();
        return ResponseEntity.ok(ExchangeRateDto.builder()
                .rate(rate.getRate())
                .source(rate.getSource())
                .fetchedAt(rate.getFetchedAt())
                .build());
    }
}
