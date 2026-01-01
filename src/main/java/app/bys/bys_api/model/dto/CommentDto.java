package app.bys.bys_api.model.dto;

import app.bys.bys_api.error.ErrorMessage;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Setter
@Getter
@Builder
public class CommentDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("text")
    @NotBlank(message = ErrorMessage.EM_EMPTY_FIELD)
    @Size(min = 10, max = 1000, message = "The comment must be between 10 and 1000 characters long.")
    private String text;

    @JsonProperty(value = "comment_date", access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime commentDate;

    @JsonProperty(value = "author_id", access = JsonProperty.Access.READ_ONLY)
    private Long author;

    @JsonProperty(value = "provider_id", access = JsonProperty.Access.READ_ONLY)
    private Long provider;

    @JsonProperty("request_id")
    @NotNull(message = ErrorMessage.EM_EMPTY_FIELD)
    private Long request;

}
