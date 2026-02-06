package app.bys.bys_api.model.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "comment")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "text")
    private String text;

    @Column(name = "comment_date")
    private LocalDateTime commentDate;

    @Column(name = "star_rating", nullable = false)
    @Min(value = 0)
    @Max(value = 5)
    private Double starRating;

    @ManyToOne
    @JoinColumn(name = "author_id")
    private FinalUser author;

    @ManyToOne
    @JoinColumn(name = "provider_id")
    private ServiceProvider provider;

    @OneToOne
    @JoinColumn(name = "request_id", unique = true)
    private ServiceRequest request;
}
