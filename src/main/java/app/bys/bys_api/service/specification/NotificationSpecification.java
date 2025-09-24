package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Notification;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class NotificationSpecification extends ASpecification<Notification> {

    public NotificationSpecification(SearchCriteria criteria) {
        super(criteria);
    }

    @Override
    public Predicate toSearchPredicate(@NonNull Root<Notification> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        return
                builder.like(
                        builder.lower(
                                root.get(
                                        "message"
                                )
                        ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

    public static Specification<Notification> hasProvider(List<Long> providerIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Notification, ServiceProvider> courses = root.join("serviceProvider");
            return criteriaBuilder.in(courses.get("id")).value(providerIdList);
        };
    }

    public static Specification<Notification> hasUser(List<Long> finalUserIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Notification, FinalUser> courses = root.join("finalUser");
            return criteriaBuilder.in(courses.get("id")).value(finalUserIdList);
        };
    }
}
