package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.Province;
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
    private String phoneNumber;
    private Province address;
    private Set<String> specializations;
    private String experience;
    private Boolean verified;
    private MembershipType membershipType;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    private Integer completedServices;
    private Double qualification;
    private String role;

    public ServiceProviderSummary(Long id, String name, String email, String phoneNumber,
                                  Province address, String experience, Boolean verified, MembershipType membershipType,
                                  LocalDateTime registrationDate, LocalDateTime lastLoginDate, Integer completedServices,
                                  Double qualification, String role) {

        this.id = id;
        this.name = name;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.address = address;
        this.experience = experience;
        this.verified = verified;
        this.membershipType = membershipType;
        this.registrationDate = registrationDate;
        this.lastLoginDate = lastLoginDate;
        this.completedServices = completedServices;
        this.qualification = qualification;
        this.role = role;
        this.specializations = new HashSet<>();
    }

}
