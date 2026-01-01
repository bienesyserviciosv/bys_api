package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class UpdateCommentDto {

    @JsonProperty("text")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    private String text;
}
