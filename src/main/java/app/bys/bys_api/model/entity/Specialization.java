package app.bys.bys_api.model.entity;

import app.bys.bys_api.model.enums.SpecializationType;
import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Specialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "specialization_type", nullable = false, unique = true)
    private SpecializationType specializationType;

    @ManyToOne
    @JoinColumn(name = "service_provider_id")
    private ServiceProvider serviceProvider;

    @OneToMany(mappedBy = "specialization", fetch = FetchType.LAZY)
    private Set<ServiceRequest> serviceRequestSet;
}
