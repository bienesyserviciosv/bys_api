package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class ServiceProviderWithPictureFlatDto {

    private Long id;
    private String name;
    private String email;
    private Boolean emailVerified;
    private String phoneNumber;
    private Province address;
    private String experience;
    private Boolean adminVerified;
    private MembershipType membershipType;
    private LocalDateTime registrationDate;
    private LocalDateTime lastLoginDate;
    private int completedServices;
    private double qualification;
    private String profilePicture;
    private UserStatus status;
    private String role;
    private String fcmToken;
}
