package app.bys.bys_api.model.enums;

public enum ExchangeRateSource {
    // Leida automaticamente de la pagina del BCV
    BCV,
    // Leida de la fuente de respaldo configurada (exchange-rate.fallback.url)
    FALLBACK,
    // Fijada a mano por un super administrador
    MANUAL
}
