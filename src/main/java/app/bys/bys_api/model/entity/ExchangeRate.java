package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.ExchangeRateSource;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Historial de la tasa oficial USD -> VES (Bs por 1 USD). La ultima fila es la vigente.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "exchange_rate")
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal rate;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ExchangeRateSource source;

    @Column(name = "fetched_at", nullable = false)
    private LocalDateTime fetchedAt;

    @Column(name = "created_by")
    private String createdBy;
}
