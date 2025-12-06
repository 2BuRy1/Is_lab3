package systems.project.configuratons;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.logging.Logger;

@Configuration
public class MinioConfiguration {

    private final StorageProperties properties;
    private final Logger logger;
    private MinioClient client;

    public MinioConfiguration(StorageProperties properties, Logger logger) {
        this.properties = properties;
        this.logger = logger;
    }

    @Bean
    public MinioClient minioClient() {
        if (client == null) {
            client = MinioClient.builder()
                    .endpoint(properties.getEndpoint())
                    .credentials(properties.getAccessKey(), properties.getSecretKey())
                    .build();
        }
        return client;
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            MinioClient minioClient = minioClient();
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder()
                    .bucket(properties.getBucket())
                    .build());
            if (!exists) {
                MakeBucketArgs.Builder builder = MakeBucketArgs.builder()
                        .bucket(properties.getBucket());
                if (properties.getRegion() != null && !properties.getRegion().isBlank()) {
                    builder.region(properties.getRegion());
                }
                minioClient.makeBucket(builder.build());
            }
        } catch (Exception e) {
            logger.warning("MinIO bucket check failed: " + e.getMessage());
        }
    }
}
