package app.bys.bys_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.time.LocalDateTime;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class FinalUserMetricsDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("name")
    private String name;

    @JsonProperty("email")
    private String email;

    @JsonProperty("phone_number")
    private String phoneNumber;

    @JsonProperty("registration_date")
    private LocalDateTime registrationDate;

    @JsonProperty("last_login_date")
    private LocalDateTime lastLoginDate;

    @JsonProperty("total_request")
    private Long totalRequests;

    @JsonProperty("accepted_requests")
    private Long acceptedRequests;

    @JsonProperty("rejected_requests")
    private Long rejectedRequests;

    @JsonProperty("pending_requests")
    private Long pendingRequests;

}
