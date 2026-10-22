package app.bys.bys_api.service.exchange;

import app.bys.bys_api.error.ExchangeRateUnavailableException;
import app.bys.bys_api.model.entity.ExchangeRate;
import app.bys.bys_api.model.enums.ExchangeRateSource;
import app.bys.bys_api.repository.ExchangeRateRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Tasa oficial USD -> VES usada para cobrar en bolivares.
 * Fuente principal: pagina del BCV. Respaldo: API JSON configurable. Ultimo recurso: tasa fijada a mano
 * por un super administrador. La tasa usada en cada cobro se guarda junto al intento de pago.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    // En la portada del BCV la tasa del dolar esta en: <div id="dolar"> ... <strong> 36,12340000 </strong>
    private static final Pattern BCV_USD_PATTERN =
            Pattern.compile("id=\"dolar\".*?<strong>\\s*([0-9.,]+)\\s*</strong>", Pattern.DOTALL);

    private final ExchangeRateRepository exchangeRateRepository;
    private final ObjectMapper objectMapper;

    @Value("${exchange-rate.bcv.url}")
    private String bcvUrl;

    @Value("${exchange-rate.fallback.url}")
    private String fallbackUrl;

    @Value("${exchange-rate.fallback.json-field}")
    private String fallbackJsonField;

    @Value("${exchange-rate.timeout-ms}")
    private int timeoutMs;

    @Value("${exchange-rate.max-age-hours}")
    private int maxAgeHours;

    @Value("${exchange-rate.max-auto-change-percent}")
    private int maxAutoChangePercent;

    @EventListener(ApplicationReadyEvent.class)
    @Async
    public void refreshOnStartup() {
        refresh();
    }

    @Scheduled(cron = "${exchange-rate.refresh-cron}", zone = "America/Caracas")
    public void scheduledRefresh() {
        refresh();
    }

    /** Ultima tasa guardada, sin importar su antiguedad. */
    public Optional<ExchangeRate> getLatest() {
        return exchangeRateRepository.findTopByOrderByFetchedAtDescIdDesc();
    }

    /** Tasa vigente para cobrar. Falla si no hay tasa o si es demasiado vieja. */
    public ExchangeRate getCurrentOrThrow() {
        ExchangeRate latest = getLatest()
                .orElseThrow(() -> new ExchangeRateUnavailableException("No hay una tasa de cambio disponible"));
        if (latest.getFetchedAt().isBefore(LocalDateTime.now().minusHours(maxAgeHours))) {
            throw new ExchangeRateUnavailableException("La tasa de cambio esta desactualizada (" + latest.getFetchedAt() + ")");
        }
        return latest;
    }

    /** Convierte USD a Bs con la tasa dada, redondeando a 2 decimales. */
    public static BigDecimal toBolivars(BigDecimal amountUsd, BigDecimal rate) {
        return amountUsd.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public synchronized ExchangeRate setManualRate(BigDecimal rate, String createdBy) {
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("La tasa debe ser mayor que cero");
        }
        ExchangeRate saved = exchangeRateRepository.save(ExchangeRate.builder()
                .rate(rate)
                .source(ExchangeRateSource.MANUAL)
                .fetchedAt(LocalDateTime.now())
                .createdBy(createdBy)
                .build());
        log.info("Tasa de cambio fijada manualmente por {}: {}", createdBy, rate);
        return saved;
    }

    /** Intenta leer la tasa del BCV y, si falla, de la fuente de respaldo. Devuelve la tasa guardada, si hubo. */
    public synchronized Optional<ExchangeRate> refresh() {
        BigDecimal rate = null;
        ExchangeRateSource source = null;
        try {
            rate = fetchFromBcv();
            source = ExchangeRateSource.BCV;
        } catch (Exception e) {
            log.warn("No se pudo leer la tasa del BCV ({}): {}", bcvUrl, e.getMessage());
        }
        if (rate == null && fallbackUrl != null && !fallbackUrl.isBlank()) {
            try {
                rate = fetchFromFallback();
                source = ExchangeRateSource.FALLBACK;
            } catch (Exception e) {
                log.warn("No se pudo leer la tasa de respaldo ({}): {}", fallbackUrl, e.getMessage());
            }
        }
        if (rate == null) {
            log.error("No se pudo actualizar la tasa de cambio; se mantiene la ultima guardada");
            return Optional.empty();
        }
        return Optional.of(store(rate, source));
    }

    private ExchangeRate store(BigDecimal rate, ExchangeRateSource source) {
        LocalDateTime now = LocalDateTime.now();
        Optional<ExchangeRate> latest = getLatest();

        if (latest.isPresent()) {
            ExchangeRate previous = latest.get();
            if (previous.getRate().compareTo(rate) == 0 && previous.getSource() != ExchangeRateSource.MANUAL) {
                // Misma tasa: solo se marca como vigente (el BCV no publica los fines de semana)
                previous.setFetchedAt(now);
                return exchangeRateRepository.save(previous);
            }
            BigDecimal changePercent = rate.subtract(previous.getRate()).abs()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(previous.getRate(), 2, RoundingMode.HALF_UP);
            if (changePercent.compareTo(BigDecimal.valueOf(maxAutoChangePercent)) > 0) {
                log.error("La tasa leida ({}) difiere {}% de la anterior ({}); no se guarda automaticamente. "
                        + "Si es correcta, fijarla a mano desde el panel.", rate, changePercent, previous.getRate());
                return previous;
            }
        }

        ExchangeRate saved = exchangeRateRepository.save(ExchangeRate.builder()
                .rate(rate)
                .source(source)
                .fetchedAt(now)
                .createdBy("system")
                .build());
        log.info("Tasa de cambio actualizada desde {}: {}", source, rate);
        return saved;
    }

    private BigDecimal fetchFromBcv() throws Exception {
        String html = httpGet(bcvUrl);
        Matcher matcher = BCV_USD_PATTERN.matcher(html);
        if (!matcher.find()) {
            throw new IllegalStateException("No se encontro la tasa del dolar en la pagina del BCV");
        }
        return parseVenezuelanNumber(matcher.group(1));
    }

    private BigDecimal fetchFromFallback() throws Exception {
        JsonNode json = objectMapper.readTree(httpGet(fallbackUrl));
        JsonNode value = json.get(fallbackJsonField);
        if (value == null || value.isNull()) {
            throw new IllegalStateException("La respuesta de respaldo no tiene el campo " + fallbackJsonField);
        }
        BigDecimal rate = value.isNumber() ? value.decimalValue() : parseVenezuelanNumber(value.asText());
        if (rate.signum() <= 0) {
            throw new IllegalStateException("Tasa de respaldo invalida: " + rate);
        }
        return rate;
    }

    private String httpGet(String url) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(timeoutMs))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("User-Agent", "Mozilla/5.0 (BYS tasa BCV)")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException("HTTP " + response.statusCode());
        }
        return response.body();
    }

    /** "36,12340000" o "1.234,56" -> BigDecimal. Si no hay coma, se interpreta con punto decimal. */
    static BigDecimal parseVenezuelanNumber(String raw) {
        String text = raw.trim();
        if (text.contains(",")) {
            text = text.replace(".", "").replace(",", ".");
        }
        BigDecimal value = new BigDecimal(text);
        if (value.signum() <= 0) {
            throw new IllegalStateException("Tasa invalida: " + raw);
        }
        return value;
    }
}
