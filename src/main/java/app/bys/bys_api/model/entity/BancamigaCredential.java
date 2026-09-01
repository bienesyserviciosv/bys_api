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
@Table(name = "bancamiga_credential")
public class BancamigaCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "access_token", length = 2048)
    private String accessToken;

    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

    @Column(name = "token_issued_at")
    private LocalDateTime tokenIssuedAt;

    @Column(name = "token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @Column(name = "last_refreshed_at")
    private LocalDateTime lastRefreshedAt;

    @Column(name = "last_refresh_attempt_at")
    private LocalDateTime lastRefreshAttemptAt;

    @Column(name = "last_refresh_error", length = 1024)
    private String lastRefreshError;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
