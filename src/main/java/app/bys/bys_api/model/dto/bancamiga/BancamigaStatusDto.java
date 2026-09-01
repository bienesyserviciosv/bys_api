package app.bys.bys_api.model.dto.bancamiga;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BancamigaStatusDto {

    @JsonProperty("enabled")
    private boolean enabled;

    @JsonProperty("has_credentials")
    private boolean hasCredentials;

    @JsonProperty("token_issued_at")
    private LocalDateTime tokenIssuedAt;

    @JsonProperty("token_expires_at")
    private LocalDateTime tokenExpiresAt;

    @JsonProperty("last_refreshed_at")
    private LocalDateTime lastRefreshedAt;

    @JsonProperty("last_refresh_attempt_at")
    private LocalDateTime lastRefreshAttemptAt;

    @JsonProperty("last_refresh_error")
    private String lastRefreshError;
}
