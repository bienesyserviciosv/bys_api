package app.bys.bys_api.model.dto;

import app.bys.bys_api.model.enums.PictureType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class PictureDto {

    @JsonProperty("id")
    private Long id;

    @JsonProperty("url")
    private String url;

    @JsonProperty("picture_type")
    private PictureType pictureType;

}
