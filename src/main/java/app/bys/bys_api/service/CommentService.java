package app.bys.bys_api.service;

import app.bys.bys_api.error.ForbiddenActionException;
import app.bys.bys_api.mapper.CommentMapper;
import app.bys.bys_api.mapper.PageMapper;
import app.bys.bys_api.model.dto.CommentDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.UpdateCommentDto;
import app.bys.bys_api.model.entity.Comment;
import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.repository.CommentRepository;
import app.bys.bys_api.repository.FinalUserRepository;
import app.bys.bys_api.repository.ServiceRequestRepository;
import app.bys.bys_api.service.specification.CommentSpecification;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final FinalUserRepository finalUserRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final CommentMapper commentMapper;

    public CommentDto get(Long id) {
        return commentMapper.entityToDto(commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment with id: " + id + " not found")));
    }

    public PageDto<CommentDto> getAll(Pageable pageable, String search, List<Long> authorIdList,
                                      List<Long> requestIdList, List<Long> providerIdList) {
        List<Specification<Comment>> specificationList = getSpecificationList(search, authorIdList, requestIdList, providerIdList);
        Page<Comment> page = commentRepository.findAll(
                Specification.allOf(specificationList.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
                ),
                pageable);
        Page<CommentDto> dtoPage = page.map(commentMapper::entityToDto);
        return PageMapper.pageToDto(dtoPage);
    }

    private List<Specification<Comment>> getSpecificationList(String search, List<Long> authorIdList, List<Long> requestIdList, List<Long> providerIdList) {
        Specification<Comment> providerSpec =
                providerIdList != null ? CommentSpecification.hasProvider(providerIdList)
                        : null;

        Specification<Comment> userSpec =
                authorIdList != null ? CommentSpecification.hasUser(authorIdList)
                        : null;

        Specification<Comment> serviceRequestSpec =
                requestIdList != null ? CommentSpecification.hasServiceRequest(requestIdList)
                        : null;

        CommentSpecification searchSpec =
                search != null ? new CommentSpecification(
                        new SearchCriteria(
                                "text",
                                "s",
                                search
                        )
                )
                        : null;

        return new ArrayList<>(Arrays.asList(
                providerSpec,
                userSpec,
                serviceRequestSpec,
                searchSpec
        ));

    }

    public CommentDto create(CommentDto commentDto, String email) {
        FinalUser user = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final User with email: " + email + " not found"));
        ServiceRequest request = serviceRequestRepository.findById(commentDto.getRequest())
                .orElseThrow(() -> new EntityNotFoundException("Service Request with id: " + commentDto.getRequest() + " not found"));
        ServiceProvider provider = request.getServiceProvider();

        if (provider==null) {
            throw new RuntimeException("The service request doesn't have a provider assigned");
        }

        if (!request.getFinalUser().getId().equals(user.getId())) {
            throw new ForbiddenActionException("The request does not belong to this user");
        }

        if (request.getComment() != null) {
            throw new ForbiddenActionException("This request already has a comment");
        }

        Comment comment = Comment.builder()
                .text(commentDto.getText())
                .commentDate(LocalDateTime.now())
                .author(user)
                .provider(provider)
                .request(request)
                .build();

        request.setComment(comment);

        return commentMapper.entityToDto(commentRepository.save(comment));
    }

    public CommentDto update(String email, Long id, UpdateCommentDto updateCommentDto) {
        FinalUser storedUser = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final User with email: " + email + " not found"));
        Comment storedComment = commentRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Comment with id: " + id + " not found"));

        if (!storedComment.getAuthor().getId().equals(storedUser.getId())) {
            throw new ForbiddenActionException("The request does not belong to this user");
        }

        storedComment.setText(updateCommentDto.getText());
        storedComment.setCommentDate(LocalDateTime.now());

        return commentMapper.entityToDto(commentRepository.save(storedComment));
    }

    public void delete(Long commentId, String email) {

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new EntityNotFoundException("Comment not found"));
        FinalUser user = finalUserRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("Final User with email: " + email + " not found"));

        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ForbiddenActionException("You cannot delete a comment you did not create");
        }

        ServiceRequest request = comment.getRequest();
        if (request != null) {
            request.setComment(null);
            serviceRequestRepository.save(request);
        }

        commentRepository.delete(comment);
    }




    /*Set<Comment>commentUserSet = user.getCommentSet();
        commentUserSet.add(comment);
        user.setCommentSet(commentUserSet);
        finalUserRepository.save(user);

        Set<Comment>commentProviderSet = provider.getCommentSet();
        commentProviderSet.add(comment);
        provider.setCommentSet(commentProviderSet);
        serviceProviderRepository.save(provider);

        request.setComment(comment);
        serviceRequestRepository.save(request);*/


}
