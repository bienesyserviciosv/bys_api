package app.bys.bys_api.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "offer")
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "price", nullable = false)
    private double price;

    @Column(name = "duration", nullable = false)
    private String duration;

    @Column(name = "description")
    private String description;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    private ServiceProvider provider;

    @ManyToOne
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne
    @JoinColumn(name = "final_user_id")
    private FinalUser finalUser;

    @Builder.Default
    @Column(name = "accepted")
    private Boolean accepted = false;

    @OneToOne(mappedBy = "offer")
    private Payment payment;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "accepted_at")
    private LocalDateTime acceptedAt;

}
