package app.bys.bys_api.service.specification;

import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.springframework.data.jpa.domain.Specification;

@AllArgsConstructor
public abstract class ASpecification<T> implements Specification<T> {
    public SearchCriteria criteria;

    public Predicate toPredicate(@NonNull Root<T> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        if (!criteria.getOperation().equalsIgnoreCase("s")) {
            if (criteria.getOperation().equalsIgnoreCase("in")) {
                return builder.in(root.get(criteria.getKey())).value(criteria.getValue());
            } else if (criteria.getOperation().equalsIgnoreCase(":")) {
                if (root.get(criteria.getKey()).getJavaType() == String.class) {
                    return builder.like(
                            root.get(criteria.getKey()), "%" + criteria.getValue() + "%");
                } else {
                    return builder.equal(root.get(criteria.getKey()), criteria.getValue());
                }
            }
        }
        else {
            return toSearchPredicate(root, query, builder);
        }
        return null;
    }
    public abstract Predicate toSearchPredicate(@NonNull Root<T> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder);

}
