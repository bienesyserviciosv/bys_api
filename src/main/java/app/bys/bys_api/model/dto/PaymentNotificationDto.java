package app.bys.bys_api.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentNotificationDto {

    private Long paymentId;
    private String payerName;
    private Double amount;
    private LocalDateTime timestamp;
}
