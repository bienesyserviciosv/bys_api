package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.UserStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ServiceProviderSummary {

    private Long id;
    private String name;
    private String email;
    private Boolean emailVerified;
    private String phoneNumber;
    private Province address;
    private Set<String> specializations;
    private String experience;
    private Boolean adminVerified;
    private MembershipType membershipType;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    private Integer completedServices;
    private Double qualification;
    private UserStatus status;
    private String role;

    public ServiceProviderSummary(Long id, String name, String email, Boolean emailVerified, String phoneNumber,
                                  Province address, String experience, Boolean adminVerified, MembershipType membershipType,
                                  LocalDateTime registrationDate, LocalDateTime lastLoginDate, Integer completedServices,
                                  Double qualification, UserStatus status, String role) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.emailVerified = emailVerified;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.experience = experience;
        this.adminVerified = adminVerified;
        this.membershipType = membershipType;
        this.registrationDate = registrationDate;
        this.lastLoginDate = lastLoginDate;
        this.completedServices = completedServices;
        this.qualification = qualification;
        this.status = status;
        this.role = role;
        this.specializations = new HashSet<>();
    }

}
