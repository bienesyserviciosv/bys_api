package app.bys.bys_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class CommentQueryDto {
    private Long id;
    private String text;
    private LocalDateTime commentDate;
    private Long authorId;
    private Long providerId;
    private Long requestId;
}
