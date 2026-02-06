package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.*;
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

    @JsonProperty("star_rating")
    @Min(value = 0)
    @Max(value = 5)
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD)
    private Double starRating;
}
