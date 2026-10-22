package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.C2pAttemptStatus;
import app.bys.bys_api.model.enums.PhoneCode;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cada intento de cobro C2P queda registrado aqui (aprobado o no), para:
 * limitar los intentos fallidos por oferta, conciliar los cobros sin respuesta y auditar.
 * Nunca se guarda la clave OTP.
 */
@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "c2p_attempt")
public class C2pAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "final_user_id", nullable = false)
    private FinalUser finalUser;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id")
    private Payment payment;

    // Codigo del banco del pagador (ej. "0102")
    @Column(name = "bank_code", nullable = false)
    private String bankCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "phone_code")
    private PhoneCode phoneCode;

    @Column(name = "phone_number")
    private String phoneNumber;

    // Telefono del pagador en formato Bancamiga: 58 + operadora sin 0 + numero (ej. 584141234567).
    // Se borra (null) si el usuario elimina su cuenta.
    @Column(name = "phone")
    private String phone;

    // Documento del pagador: letra + numero (ej. V12345678). Se borra (null) si el usuario elimina su cuenta.
    @Column(name = "id_document")
    private String idDocument;

    @Column(name = "amount_usd", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountUsd;

    @Column(name = "exchange_rate", nullable = false, precision = 19, scale = 8)
    private BigDecimal exchangeRate;

    @Column(name = "amount_bs", nullable = false, precision = 19, scale = 2)
    private BigDecimal amountBs;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private C2pAttemptStatus status;

    @Column(name = "bancamiga_id")
    private String bancamigaId;

    @Column(name = "bancamiga_nroref")
    private String bancamigaNroref;

    @Column(name = "bancamiga_dataorig")
    private String bancamigaDataorig;

    @Column(name = "error_code")
    private String errorCode;

    @Column(name = "error_message", length = 1024)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
