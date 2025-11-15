package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class NotificationDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("message")
    private String message;

    @JsonProperty("read")
    private boolean read;

    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    @JsonProperty("service_provider_id")
    private Long serviceProviderId;

    @JsonProperty("service_request_id")
    private Long serviceRequestId;

    @JsonProperty("final_user_id")
    private Long finalUserId;

    @JsonProperty("payment_id")
    private Long paymentId;

    @JsonProperty("notification_type")
    private NotificationType notificationType;

}
