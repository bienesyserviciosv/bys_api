package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.ServiceRequest;
import app.bys.bys_api.model.entity.Specialization;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

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
                                        "name"
                                )
                        ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

    public static Specification<ServiceRequest> hasSpecialization(Long specializationId) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<ServiceRequest, Specialization> courses = root.join("specialization");
            return criteriaBuilder.in(courses.get("id")).value(specializationId);
        };
    }

}
