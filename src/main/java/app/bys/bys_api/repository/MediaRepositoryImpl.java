package app.bys.bys_api.repository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;

@Repository
public class MediaRepositoryImpl implements MediaRepository{

    @Value("${media.bucket}")
    private String bucketName;

    private final S3Client s3Client;

    public MediaRepositoryImpl(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @Override
    public void saveImage(String imageName, MultipartFile image) throws IOException {

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .contentType("image/jpeg")
                .key(imageName)
                .build();

        s3Client.putObject(objectRequest, RequestBody.fromInputStream(image.getInputStream(), image.getSize()));
    }

    @Override
    public void deleteImage(String imageName) {

    }
}
