package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.entity.Role;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("roles")
    private Set<Role> roles;

    @JsonProperty("registration_date")
    private LocalDateTime registrationDate;

    @JsonProperty("token")
    private String token;

    @JsonProperty("fcm_token")
    private String fcmToken;

}
