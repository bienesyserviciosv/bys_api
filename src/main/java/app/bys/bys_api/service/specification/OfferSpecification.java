package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class OfferSpecification extends ASpecification<Offer> {

    public OfferSpecification(SearchCriteria criteria) {
        super(criteria);
    }

    @Override
    public Predicate toSearchPredicate(@NonNull Root<Offer> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        return
                builder.like(
                        builder.lower(
                                root.get(
                                        "name"
                                )
                        ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

    public static Specification<Offer> hasProvider(List<Long> providerIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Offer, ServiceProvider> courses = root.join("provider");
            return criteriaBuilder.in(courses.get("id")).value(providerIdList);
        };
    }
}
