package app.bys.bys_api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AWSConfig {

    @Value("${config.aws.access_key}")
    private String accessKey;

    @Value("${config.aws.secret_access_key}")
    private String secretAccessKey;

    public AwsCredentialsProvider credentialsProvider() {
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(accessKey, secretAccessKey)
        );
    }

    @Bean
    public S3Client amazonS3() {
        return S3Client.builder()
                .credentialsProvider(credentialsProvider())
                .region(Region.US_EAST_1)
                .build();
    }
}
