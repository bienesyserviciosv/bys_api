package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.ServiceCatalog;
import app.bys.bys_api.model.entity.Specialization;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class ServiceCatalogSpecification extends ASpecification<ServiceCatalog>{

    public ServiceCatalogSpecification(SearchCriteria searchCriteria) {super(searchCriteria);}

    @Override
    public Predicate toSearchPredicate(@NonNull Root<ServiceCatalog> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        return builder.like(
                builder.lower(
                        root.get(
                                "name"
                        )
                ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

    public static Specification<ServiceCatalog> hasSpecialization(List<Long> specializationIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<ServiceCatalog, Specialization> courses = root.join("specialization");
            return criteriaBuilder.in(courses.get("id")).value(specializationIdList);
        };
    }

}
