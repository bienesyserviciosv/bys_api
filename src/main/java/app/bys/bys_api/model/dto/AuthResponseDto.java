package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.entity.Role;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AuthResponseDto {
    private String username;
    private String token;
    private Set<Role> roles;
}
