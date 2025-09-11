package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Payment;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.*;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class PaymentSpecification extends ASpecification<Payment>{


    public PaymentSpecification(SearchCriteria criteria) {
        super(criteria);
    }

    @Override
    public Predicate toSearchPredicate(@NonNull Root<Payment> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        return
                builder.like(
                        builder.lower(
                                root.get(
                                        "bank"
                                )
                        ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

    public static Specification<Payment> hasUser(List<Long> userIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Payment, FinalUser> courses = root.join("finalUser");
            return criteriaBuilder.in(courses.get("id")).value(userIdList);
        };
    }

    public static Specification<Payment> hasProvider(List<Long> providerIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Payment, ServiceProvider> courses = root.join("serviceProvider");
            return criteriaBuilder.in(courses.get("id")).value(providerIdList);
        };
    }


}
