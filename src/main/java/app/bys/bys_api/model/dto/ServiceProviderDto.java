package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.Level;
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

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ServiceProviderDto {

    @JsonProperty("id")
    private Long id;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @Size(min = 3, max = 20, groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("name")
    private String name;

    @Email(message = ErrorMessage.EM_WRONG_EMAIL, groups = {OnCreate.class, OnUpdate.class})
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @JsonProperty("email")
    private String email;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @JsonProperty("phoneNumber")
    private String phoneNumber;

    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD, groups = OnCreate.class)
    @JsonProperty("experience")
    private String experience;

    @JsonProperty("verified")
    private Boolean verified = false;

    @JsonProperty("level")
    @Enumerated(EnumType.STRING)
    private Level level = Level.NOT_VERIFIED;

    @JsonProperty("latitude")
    private String latitude;

    @JsonProperty("longitude")
    private String longitude;

    @JsonProperty("qualification")
    private double qualification;


}
