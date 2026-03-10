package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.RequestStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ServiceRequestMinimal {

    private Long id;
    private RequestStatus status;
    private int offerQuantity;
    private Long userId;
}
