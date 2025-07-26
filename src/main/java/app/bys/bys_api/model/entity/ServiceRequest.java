package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
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

    @Column(name = "date_time")
    private Date dateTime;

    @Column(name = "status")
    private Status status;

    @ManyToOne
    @JoinColumn(name = "specialization_id")
    private Specialization specialization;

    @OneToMany(mappedBy = "serviceRequest",fetch = FetchType.LAZY)
    private Set<Picture> pictureSet;

    @ManyToOne
    @JoinColumn(name = "final_user_id")
    private FinalUser finalUser;

    @ManyToOne
    @JoinColumn(name = "service_provider")
    private ServiceProvider serviceProvider;
}
