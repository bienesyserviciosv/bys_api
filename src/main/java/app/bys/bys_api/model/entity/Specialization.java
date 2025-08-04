package app.bys.bys_api.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "specialization")
public class Specialization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "specialization_type", nullable = false, unique = true)
    private String specializationType;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "specializations")
    private Set<ServiceProvider> serviceProviderSet;

    @OneToMany(mappedBy = "specialization", fetch = FetchType.LAZY)
    private Set<ServiceRequest> serviceRequestSet;
}
