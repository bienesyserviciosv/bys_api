package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class UpdateCommentDto {

    @JsonProperty("text")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Size(min = 10, max = 1000, message = "The comment must be between 10 and 1000 characters long.")
    private String text;
}
