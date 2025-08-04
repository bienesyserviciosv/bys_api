package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.Specialization;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ServiceProviderSpecification extends ASpecification<ServiceProvider> {

    public ServiceProviderSpecification(SearchCriteria criteria) {
        super(criteria);
    }

    @Override
    public Predicate toSearchPredicate(@NonNull Root<ServiceProvider> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        return
                builder.like(
                        builder.lower(
                                root.get(
                                        "name"
                                )
                        ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

    public static Specification<ServiceProvider> hasSpecialization(List<Long> specializationIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<ServiceProvider, Specialization> courses = root.join("specializations");
            return criteriaBuilder.in(courses.get("id")).value(specializationIdList);
        };
    }


}
