package app.bys.bys_api.service;

import app.bys.bys_api.error.ForbiddenActionException;
import app.bys.bys_api.error.InvalidStarRatingException;
import app.bys.bys_api.mapper.CommentMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.CommentDto;
import app.bys.bys_api.model.dto.CommentQueryDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.UpdateCommentDto;
import app.bys.bys_api.model.entity.Comment;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.enums.RequestStatus;
import app.bys.bys_api.repository.CommentRepository;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceProviderRepository;
import app.bys.bys_api.repository.ServiceRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final FinalUserRepository finalUserRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final CommentMapper commentMapper;
    private final ServiceProviderRepository serviceProviderRepository;
    private final ServiceProviderService serviceProviderService;

    public CommentDto get(Long id) {
        CommentQueryDto commentQueryDto = commentRepository.findCommentById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment with id: " + id + " not found"));

        return CommentDto.builder()
                .id(commentQueryDto.getId())
                .text(commentQueryDto.getText())
                .commentDate(commentQueryDto.getCommentDate())
                .starRating(commentQueryDto.getStartRating())
                .author(commentQueryDto.getAuthorId())
                .provider(commentQueryDto.getProviderId())
                .request(commentQueryDto.getRequestId())
                .build();
    }

    public PageDto<CommentDto> getAll(Pageable pageable, String search, List<Long> authorIdList,
                                      List<Long> providerIdList, List<Long> requestIdList) {

        if (search == null || search.isBlank()) search = "";

        Page<CommentQueryDto> page = commentRepository.searchComments(
                authorIdList,
                providerIdList,
                requestIdList,
                search,
                pageable
        );

        Page<CommentDto> dtoPage = page.map(c -> CommentDto.builder()
                .id(c.getId())
                .text(c.getText())
                .commentDate(c.getCommentDate())
                .starRating(c.getStartRating())
                .author(c.getAuthorId())
                .provider(c.getProviderId())
                .request(c.getRequestId())
                .build()
        );

        return PageMapper.pageToDto(dtoPage);
    }

    public CommentDto create(CommentDto commentDto, String email) {
        FinalUser user = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final User with email: " + email + " not found"));
        ServiceRequest request = serviceRequestRepository.findById(commentDto.getRequest())
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " + commentDto.getRequest() + " not found"));

        ServiceProvider provider = request.getServiceProvider();

        if (provider == null) {
            throw new RuntimeException("The service request doesn't have a provider assigned");
        }

        if (!request.getFinalUser().getId().equals(user.getId())) {
            throw new ForbiddenActionException("The request does not belong to this user");
        }

        if (request.getComment() != null) {
            throw new ForbiddenActionException("This request already has a comment");
        }

        if (!request.getRequestStatus().equals(RequestStatus.COMPLETED)) {
            throw new ForbiddenActionException("The request has not been completed");
        }

        double rating = commentDto.getStarRating();
        if (rating % 1 != 0 && rating % 1 != 0.5) {
            throw new InvalidStarRatingException();
        }

        Comment comment = Comment.builder()
                .text(commentDto.getText())
                .starRating(commentDto.getStarRating())
                .commentDate(LocalDateTime.now())
                .author(user)
                .provider(provider)
                .request(request)
                .build();

        Comment savedComment = commentRepository.save(comment);

        Double newAvg = commentRepository.calculateAverageRating(provider.getId());
        provider.setQualification(newAvg);
        provider.setMembershipType(serviceProviderService.calculateMembershipType(provider));
        serviceProviderRepository.save(provider);

        request.setComment(savedComment);
        return commentMapper.entityToDto(savedComment);

    }

    public CommentDto update(String email, Long id, UpdateCommentDto updateCommentDto) {
        FinalUser storedUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final User with email: " + email + " not found"));
        Comment storedComment = commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment with id: " + id + " not found"));

        if (!storedComment.getAuthor().getId().equals(storedUser.getId())) {
            throw new ForbiddenActionException("The request does not belong to this user");
        }

        double rating = updateCommentDto.getStarRating();
        if (rating % 1 != 0 && rating % 1 != 0.5) {
            throw new InvalidStarRatingException();
        }

        storedComment.setText(updateCommentDto.getText());
        storedComment.setStarRating(updateCommentDto.getStarRating());
        storedComment.setCommentDate(LocalDateTime.now());

        Comment updated = commentRepository.save(storedComment);

        ServiceProvider provider = storedComment.getProvider();
        Double newAvg = commentRepository.calculateAverageRating(provider.getId());
        provider.setQualification(newAvg);
        provider.setMembershipType(serviceProviderService.calculateMembershipType(provider));

        serviceProviderRepository.save(provider);

        return commentMapper.entityToDto(updated);
    }

    public void delete(Long commentId, String email) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));

        if (!comment.getAuthor().getEmail().equals(email)) {
            throw new ForbiddenActionException("You cannot delete a comment you did not create");
        }

        ServiceRequest request = comment.getRequest();
        request.setComment(null);
        serviceRequestRepository.save(request);

        ServiceProvider provider = comment.getProvider();
        Double newAvg = commentRepository.calculateAverageRating(provider.getId());
        provider.setQualification(newAvg);
        provider.setMembershipType(serviceProviderService.calculateMembershipType(provider));

        serviceProviderRepository.save(provider);


        commentRepository.delete(comment);
    }
}
