package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.UserStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "final_user")
public class FinalUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name")
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone_number", unique = true)
    private String phoneNumber;

    @Column(name = "password")
    private String password;

    @Builder.Default
    @Column(name = "email_verified")
    private boolean emailVerified = false;

    @Builder.Default
    @Column(name = "phone_verified")
    private boolean phoneVerified = false;

    @Column(name = "registration_time")
    private LocalDateTime registrationDate;

    @Column(name = "last_login_date")
    private LocalDateTime lastLoginDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private UserStatus status;

    @Column(name = "profile_picture")
    private String profilePicture;

    @Column(name = "total_request")
    private Long totalRequests;

//    @Column(name = "accepted_requests")
//    private Long acceptedRequests;
//
//    @Column(name = "rejected_requests")
//    private Long rejectedRequests;
//
//    @Column(name = "pending_requests")
//    private Long pendingRequests;

    @Column(name = "completed_requests")
    private Long completedRequests;

    @OneToMany(mappedBy = "finalUser", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    private Set<ServiceRequest> serviceRequestSet;

    @ManyToMany(fetch = FetchType.EAGER, targetEntity = Role.class, cascade = {CascadeType.MERGE, CascadeType.REFRESH})
    @JoinTable(name = "final_users_roles",
            joinColumns = @JoinColumn(name = "final_user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<Role> roles;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "finalUser")
    private Set<Offer> offerSet;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "finalUser")
    private Set<Payment> paymentSet;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "finalUser", cascade = CascadeType.REMOVE)
    private List<Notification> notificationList;


}
