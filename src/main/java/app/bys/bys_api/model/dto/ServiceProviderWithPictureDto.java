package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.model.enums.MembershipType;
import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("phone_number")
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

    @Builder.Default
    @JsonProperty("verified")
    private Boolean verified = false;

    @Builder.Default
    @JsonProperty("membership_type")
    @Enumerated(EnumType.STRING)
    private MembershipType membershipType = MembershipType.NOT_VERIFIED;

    @JsonProperty("registration_date")
    private LocalDateTime registrationDate;

    @Builder.Default
    @JsonProperty("completed_services")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    private int completedServices = 0;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 8)
    private String password;

    @JsonProperty("qualification")
    private double qualification;

    @JsonProperty("profile_picture")
    private String profilePicture;

    @JsonProperty("work_picture_set")
    private Set<String> workPictureSet = new HashSet<>();

    @JsonProperty(value = "roles", access = JsonProperty.Access.READ_ONLY)
    private Set<Role> roles;

}
