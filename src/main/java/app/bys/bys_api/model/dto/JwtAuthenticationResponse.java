package app.bys.bys_api.model.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class JwtAuthenticationResponse {
    private String token;
}
