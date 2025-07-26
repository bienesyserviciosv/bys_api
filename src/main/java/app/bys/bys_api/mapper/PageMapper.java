package app.bys.bys_api.mapper;

import app.bys.bys_api.model.dto.PageDto;
import org.springframework.data.domain.Page;

public class PageMapper {

    public static <T> PageDto<T> pageToDto(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getTotalElements());
    }
}
