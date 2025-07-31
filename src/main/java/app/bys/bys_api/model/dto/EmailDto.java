package app.bys.bys_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class EmailDto {

    @JsonProperty("email")
    private String email;

}
