package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.PictureType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "picture")
public class Picture {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "url")
    private String url;

    @Enumerated(EnumType.STRING)
    @Column(name = "picture_type")
    private PictureType pictureType;

    @ManyToOne
    @JoinColumn(name = "service_request_id")
    private ServiceRequest serviceRequest;

    @ManyToOne
    @JoinColumn(name = "service_provider_id")
    private ServiceProvider serviceProvider;

    @OneToOne
    @JoinColumn(name = "final_user_id")
    private FinalUser finalUser;

    @OneToOne
    @JoinColumn(name = "payment_id")
    private Payment payment;
}
