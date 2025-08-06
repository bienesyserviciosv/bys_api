package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.entity.Specialization;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ServiceRequestSpecification extends ASpecification<ServiceRequest> {

    public ServiceRequestSpecification(SearchCriteria criteria) {
        super(criteria);
    }

    @Override
    public Predicate toSearchPredicate(@NonNull Root<ServiceRequest> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        return
                builder.like(
                        builder.lower(
                                root.get(
                                        "description"
                                )
                        ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

    public static Specification<ServiceRequest> hasSpecialization(List<Long> specializationIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<ServiceRequest, Specialization> courses = root.join("specialization");
            return criteriaBuilder.in(courses.get("id")).value(specializationIdList);
        };
    }

    public static Specification<ServiceRequest> hasUser(List<Long> userIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<ServiceRequest, FinalUser> courses = root.join("finalUser");
            return criteriaBuilder.in(courses.get("id")).value(userIdList);
        };
    }

    public static Specification<ServiceRequest> isArtist(Long userId) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<ServiceRequest, FinalUser> userJoin = root.join("user", JoinType.LEFT);
            return criteriaBuilder.in(userJoin.get("id")).value(userId);
        };
    }

}
