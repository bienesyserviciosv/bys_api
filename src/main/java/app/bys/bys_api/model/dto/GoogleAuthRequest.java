package app.bys.bys_api.model.dto;

import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class GoogleAuthRequest {

    private String idToken;
}
