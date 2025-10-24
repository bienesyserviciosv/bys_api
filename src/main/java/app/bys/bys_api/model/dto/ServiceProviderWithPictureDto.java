package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.UserStatus;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@JsonPropertyOrder({
        "id",
        "name",
        "email",
        "phoneNumber",
        "address",
        "specializations",
        "experience",
        "verified",
        "membershipType",
        "registrationDate",
        "lastLoginDate",
        "completedServices",
        "password",
        "qualification",
        "profilePicture",
        "workPictureSet",
        "role",
        "status",
        "token"
})

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ServiceProviderWithPictureDto {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 3, max = 20, message = "The length must be between 3 and 20 char", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("name")
    private String name;

    @Email(message = ErrorMessage.EM_WRONG_EMAIL, groups = {OnCreate.class, OnUpdate.class})
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @JsonProperty("email")
    private String email;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 8, groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @Enumerated(EnumType.STRING)
    @JsonProperty("address")
    private Province address;

    @JsonProperty("specializations")
    private Set<SpecializationDto> specializations;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 8, max = 1000, message = "The length must be between 8 and 1000 char", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("experience")
    private String experience;

    @JsonProperty("verified")
    private Boolean verified;

    @JsonProperty("membershipType")
    @Enumerated(EnumType.STRING)
    private MembershipType membershipType;

    @JsonProperty("registrationDate")
    private LocalDateTime registrationDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastLoginDate;

    @JsonProperty("completedServices")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private int completedServices;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 8)
    private String password;

    @JsonProperty("qualification")
    private double qualification;

    @JsonProperty("profilePicture")
    private String profilePicture;

    @JsonProperty("workPictureSet")
    private Set<String> workPictureSet = new HashSet<>();

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String role;

    @JsonProperty("status")
    private UserStatus status;

    @JsonProperty("token")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

}
