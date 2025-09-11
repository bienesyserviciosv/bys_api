package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.BankName;
import app.bys.bys_api.model.enums.PaymentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "payment")
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank")
    private BankName bank;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type")
    private PaymentType paymentType;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "id_number")
    private String idNumber;

    @Column(name = "screenshot")
    private String screenshot;

    @ManyToOne
    @JoinColumn(name = "final_user_id")
    private FinalUser finalUser;

    @ManyToOne
    @JoinColumn(name = "service_provider_id")
    private ServiceProvider serviceProvider;
}
