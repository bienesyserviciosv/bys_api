package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.*;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class CommentSpecification extends ASpecification<Comment> {

    public CommentSpecification(SearchCriteria criteria) {
        super(criteria);
    }

    @Override
    public Predicate toSearchPredicate(@NonNull Root<Comment> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        return
                builder.like(
                        builder.lower(
                                root.get(
                                        "text"
                                )
                        ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

    public static Specification<Comment> hasProvider(List<Long> providerIdList) {
        return (root, query, cb) ->
                cb.in(root.get("provider").get("id")).value(providerIdList);
    }

    public static Specification<Comment> hasUser(List<Long> userIdList) {
        return (root, query, cb) ->
                cb.in(root.get("author").get("id")).value(userIdList);
    }


    public static Specification<Comment> hasServiceRequest(List<Long> requestIdList) {
        return (root, query, cb) ->
                cb.in(root.get("request").get("id")).value(requestIdList);
    }

}
