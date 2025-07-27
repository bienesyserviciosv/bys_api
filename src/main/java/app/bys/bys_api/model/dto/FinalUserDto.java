package app.bys.bys_api.model.dto;

import app.bys.bys_api.validation.OnCreate;
import app.bys.bys_api.validation.OnUpdate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class FinalUserDto {

    @JsonProperty("id")
    private Long id;

    @NotBlank(groups = OnCreate.class)
    @Size(min = 3, max = 20, groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("name")
    private String name;

    @NotNull(groups = OnCreate.class)
    @Email(groups = {OnCreate.class, OnUpdate.class})
    @JsonProperty("email")
    private String email;

    @JsonProperty("phoneNumber")
    @NotBlank(groups = OnCreate.class)
    private String phoneNumber;
}
