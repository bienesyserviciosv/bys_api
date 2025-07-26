package app.bys.bys_api.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class PageDto<T> {

    @JsonProperty("data")
    List<T> data;

    @JsonProperty("count")
    Long count;

}