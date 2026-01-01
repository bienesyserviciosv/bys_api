package app.bys.bys_api.repository;

import app.bys.bys_api.model.dto.CommentQueryDto;
import app.bys.bys_api.model.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, JpaSpecificationExecutor<Comment> {

    @Query("""
        SELECT new app.bys.bys_api.model.dto.CommentQueryDto(
            c.id,
            c.text,
            c.commentDate,
            c.author.id,
            c.provider.id,
            c.request.id
        )
        FROM Comment c
        WHERE c.id = :id
        """)
    Optional<CommentQueryDto> findCommentById(@Param("id") Long id);

        @Query("""
        SELECT new app.bys.bys_api.model.dto.CommentQueryDto(
            c.id,
            c.text,
            c.commentDate,
            c.author.id,
            c.provider.id,
            c.request.id
        )
        FROM Comment c
        WHERE (:authorIds IS NULL OR c.author.id IN :authorIds)
          AND (:providerIds IS NULL OR c.provider.id IN :providerIds)
          AND (:requestIds IS NULL OR c.request.id IN :requestIds)
          AND (:search IS NULL OR LOWER(c.text) LIKE LOWER(CONCAT('%', :search, '%')))
        """)
        Page<CommentQueryDto> searchComments(
                @Param("authorIds") List<Long> authorIds,
                @Param("providerIds") List<Long> providerIds,
                @Param("requestIds") List<Long> requestIds,
                @Param("search") String search,
                Pageable pageable
        );
    }
