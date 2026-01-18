package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import app.bys.bys_api.model.enums.UserStatus;
import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@JsonPropertyOrder({
        "id",
        "name",
        "email",
        "phoneNumber",
        "registrationDate",
        "lastLoginDate",
        "password",
        "profilePicture",
        "role",
        "token"
})

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
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @JsonProperty("registrationDate")
    private LocalDateTime registrationDate;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime lastLoginDate;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 8, message = "The length must be greater than 8 char", groups = OnCreate.class)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @JsonProperty("profilePicture")
    private String profilePicture;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String role;

    @JsonProperty("status")
    private UserStatus status;

    @JsonProperty("totalRequest")
    private Long totalRequests;

//    @JsonProperty("acceptedRequests")
//    private Long acceptedRequests;
//
//    @JsonProperty("rejectedRequests")
//    private Long rejectedRequests;
//
//    @JsonProperty("pendingRequests")
//    private Long pendingRequests;

    @JsonProperty("completedRequests")
    private Long completedRequests;

    @JsonProperty("token")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String token;

    @JsonProperty("fcm_token")
    private String fcmToken;

}
