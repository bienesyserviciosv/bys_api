package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
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

    @Column(name = "membership_type")
    @Enumerated(EnumType.STRING)
    private MembershipType membershipType;

    @Column(name = "latitude")
    private String latitude;

    @Column(name = "longitude")
    private String longitude;

    @Column(name = "password")
    private String password;

    @Column(name = "qualification")
    private double qualification;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @Column(name = "email_verified")
    private boolean emailVerified;

    @Column(name = "phone_verified")
    private boolean phoneVerified;

    @Enumerated(EnumType.STRING)
    @Column(name = "address")
    private Province address;

    @Column(name = "completed_services")
    private int completedServices;

    @Column(name = "registration_time")
    private LocalDateTime registrationDate;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "provider_specialization",
            joinColumns = {@JoinColumn(name = "provider_id")},
            inverseJoinColumns = {@JoinColumn(name = "specialization_id")})
    private Set<Specialization> specializations;

    @OneToMany(mappedBy = "serviceProvider", fetch = FetchType.LAZY)
    private Set<ServiceRequest> serviceRequestSet;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Builder.Default
    @OneToMany(mappedBy = "serviceProvider", cascade = CascadeType.REMOVE, fetch = FetchType.LAZY)
    private Set<Picture> workPictureSet = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = Role.class, cascade = CascadeType.PERSIST)
    @JoinTable(name = "service_provider_roles",
            joinColumns = @JoinColumn(name = "service_provider_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "provider", cascade = CascadeType.REMOVE)
    private Set<Offer> offerSet;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "serviceProvider", cascade = CascadeType.REMOVE)
    private Set<Notification> notificationSet;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "serviceProvider")
    private Set<Payment> paymentSet;

}


