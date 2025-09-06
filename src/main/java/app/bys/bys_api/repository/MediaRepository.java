package app.bys.bys_api.repository;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface MediaRepository {

    void saveImage(String imageName, MultipartFile image) throws IOException;

    void deleteImage(String imageName);

}
