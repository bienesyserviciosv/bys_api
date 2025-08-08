package app.bys.bys_api.model.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "notification")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message")
    private String message;

    @Column(name = "read")
    private boolean read;

    @Column(name = "timestamp")
    private LocalDateTime timestamp;

    @ManyToOne
    @JoinColumn(name = "service_provider_id")
    private ServiceProvider recipient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", unique = false)
    private ServiceRequest serviceRequest;
}
