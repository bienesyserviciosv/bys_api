package app.bys.bys_api.model.dto;

import java.util.Map;

public record NotificationRequest(
        String token,
        String title,
        String body,
        Map<String, String> dataPayload,
        String targetEntityType,
        Long targetEntityId
) {
}
