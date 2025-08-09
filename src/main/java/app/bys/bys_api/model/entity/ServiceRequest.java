package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "service_request")
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "description")
    private String description;

    @Column(name = "latitude")
    private String latitude;

    @Column(name = "longitude")
    private String longitude;

    @Column(name = "address")
    private String address;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "request_status")
    private RequestStatus requestStatus;

    @Column(name = "offer_quantity")
    private Integer offerQuantity;

    @Column(name = "new_offer")
    private Boolean newOffer;

    @ManyToOne
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;

    @OneToMany(mappedBy = "serviceRequest",fetch = FetchType.LAZY)
    private Set<Picture> pictureSet;

    @ManyToOne
    @JoinColumn(name = "final_user_id")
    private FinalUser finalUser;

    @ManyToOne
    @JoinColumn(name = "service_provider_id")
    private ServiceProvider serviceProvider;

    @OneToMany(fetch = FetchType.LAZY)
    private Set<Offer> offerSet;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "serviceRequest")
    private List<Notification> notificationList;
}
