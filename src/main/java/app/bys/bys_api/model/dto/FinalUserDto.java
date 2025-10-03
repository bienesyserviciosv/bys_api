package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.entity.Role;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class FinalUserDto {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 3, max = 20, message = "The length must be between 3 and 20 char", groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("name")
    private String name;

    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Email(message = ErrorMessage.EM_WRONG_EMAIL, groups = {OnCreate.class, OnUpdate.class})
    @Size(max = 50, groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("email")
    private String email;

    @Size(min = 7, message = "The length must be greater than 6 char", groups = {OnCreate.class, OnUpdate.class})
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("registration_date")
    private LocalDateTime registrationDate;

    @JsonProperty(value = "last_login_date", access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastLoginDate;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 8, message = "The length must be greater than 8 char", groups = OnCreate.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @JsonProperty("profile_picture")
    private String profilePicture;

    @JsonProperty(value = "roles", access = JsonProperty.Access.READ_ONLY)
    private Set<Role> roles;
}
