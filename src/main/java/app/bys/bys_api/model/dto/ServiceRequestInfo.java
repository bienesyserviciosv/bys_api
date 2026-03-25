package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.Province;
import app.bys.bys_api.model.enums.RequestStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class ServiceRequestInfo {

    private Long id;
    private Long userId;
    private Province address;
    private String specialization;
    private Set<OfferDto> offerDtoSet;
    private RequestStatus status;
    private LocalDateTime creationDate;
    private LocalDateTime acceptanceDate;
    private LocalDateTime completedAt;
    private Double rating;
    private CommentDto commentDto;
    //private Double clientRating;
    //private CommentDto clientComment;
    private Set<String> pictureSet;
}
