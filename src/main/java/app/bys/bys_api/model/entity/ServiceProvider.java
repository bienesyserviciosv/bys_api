package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.Level;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "service_provider")
public class ServiceProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "experience")
    private String experience;

    @Column(name = "verified")
    private Boolean verified;

    @Column(name = "level")
    private Level level;

    @Column(name = "latitude")
    private String latitude;

    @Column(name = "longitude")
    private String longitude;

    @Column(name = "qualification")
    private double qualification;

    @OneToMany(mappedBy = "serviceProvider", fetch = FetchType.LAZY)
    private Set<Specialization> specializations;

    @OneToMany(mappedBy = "serviceProvider", fetch = FetchType.LAZY)
    private Set<ServiceRequest> serviceRequestSet;

    @OneToMany(mappedBy = "serviceProvider")
    private Set<Picture> pictureSet;

}


