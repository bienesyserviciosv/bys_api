package app.bys.bys_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class OfferDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("price")
    private double price;

    @JsonProperty("duration")
    private String duration;
}
