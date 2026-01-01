package app.bys.bys_api.controller;

import app.bys.bys_api.model.dto.CommentDto;
import app.bys.bys_api.model.dto.PageDto;
import app.bys.bys_api.model.dto.UpdateCommentDto;
import app.bys.bys_api.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/comment")
public class CommentController {

    private final CommentService commentService;

    @GetMapping("/{id}")
    public ResponseEntity<CommentDto> getComment(@PathVariable Long id) {
        return new ResponseEntity<>(commentService.get(id), HttpStatus.OK);
    }

    @GetMapping()
    public ResponseEntity<PageDto<CommentDto>> getAllComments(Pageable pageable,
                                              @RequestParam(required = false) String search,
                                              @RequestParam(required = false) List<Long> authorIdList,
                                              @RequestParam(required = false) List<Long> providerIdList,
                                              @RequestParam(required = false) List<Long> requestIdList) {
        return new ResponseEntity<>(commentService.getAll(pageable, search, authorIdList, providerIdList, requestIdList), HttpStatus.OK);

    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PostMapping
    public ResponseEntity<CommentDto> createComment(@Valid @RequestBody CommentDto commentDto, Authentication auth) {
        return new ResponseEntity<>(commentService.create(commentDto, auth.getName()), HttpStatus.CREATED);
    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @PatchMapping("/{id}")
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long id, @Valid @RequestBody UpdateCommentDto updateCommentDto, Authentication auth) {
        return new ResponseEntity<>(commentService.update(auth.getName(), id, updateCommentDto), HttpStatus.OK);
    }

    @PreAuthorize("hasAuthority('ROLE_USER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, Authentication auth) {
        commentService.delete(id, auth.getName());
        return new ResponseEntity<>(HttpStatus.OK);
    }

}
