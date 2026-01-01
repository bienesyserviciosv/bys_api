package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.RequestStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
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

    @Enumerated(EnumType.STRING)
    @Column(name = "address")
    private Province address;

    @Column(name = "date")
    private LocalDate date;

    @Column(name = "time")
    private LocalTime time;

    @Column(name = "request_status")
    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus;

    @Column(name = "creation_date")
    private LocalDateTime creationDate;

    @Column(name = "acceptance_date")
    private LocalDateTime acceptanceDate;

    @Column(name = "offer_quantity")
    private Integer offerQuantity;

    @Column(name = "new_offer")
    private Boolean newOffer;

    @ManyToOne
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;

    @Builder.Default
    @OneToMany(mappedBy = "serviceRequest",fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<Picture> pictureSet =  new HashSet<>();

    @ManyToOne
    @JoinColumn(name = "final_user_id")
    private FinalUser finalUser;

    @ManyToOne
    @JoinColumn(name = "service_provider_id")
    private ServiceProvider serviceProvider;

    @OneToMany(mappedBy = "serviceRequest", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<Offer> offerSet = new HashSet<>();

    @OneToOne
    @JoinColumn(name = "accepted_offer_id")
    private Offer acceptedOffer;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "serviceRequest", cascade = CascadeType.REMOVE)
    private List<Notification> notificationList;

    @OneToOne(mappedBy = "request")
    private Comment comment;
}
