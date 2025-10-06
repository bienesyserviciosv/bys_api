package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.model.entity.Offer;
import app.bys.bys_api.model.entity.ServiceProvider;
import app.bys.bys_api.model.entity.ServiceRequest;
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
                                        "description"
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

    public static Specification<Offer> hasUser(List<Long> userIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Offer, FinalUser> courses = root.join("finalUser");
            return criteriaBuilder.in(courses.get("id")).value(userIdList);
        };
    }

    public static Specification<Offer> hasServiceRequest(List<Long> serviceRequestIdList) {
        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Offer, ServiceRequest> courses = root.join("serviceRequest");
            return criteriaBuilder.in(courses.get("id")).value(serviceRequestIdList);
        };
    }

    public static Specification<Offer> isAccepted(Boolean accepted) {
        return (root, query, criteriaBuilder) -> {
            if (accepted == null) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.equal(root.get("accepted"), accepted);
        };
    }

}
