package app.bys.bys_api.service.specification;

import app.bys.bys_api.model.entity.FinalUser;
import app.bys.bys_api.utils.specification.SearchCriteria;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.NonNull;

public class FinalUserSpecification extends ASpecification<FinalUser>{

    public FinalUserSpecification(SearchCriteria criteria) {
        super(criteria);
    }

    @Override
    public Predicate toSearchPredicate(@NonNull Root<FinalUser> root, @NonNull CriteriaQuery<?> query, @NonNull CriteriaBuilder builder) {
        return
                builder.like(
                        builder.lower(
                                root.get(
                                        "name"
                                )
                        ), "%" + ((String) criteria.getValue()).toLowerCase() + "%");
    }

}
