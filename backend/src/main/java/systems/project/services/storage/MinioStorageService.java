package systems.project.services.storage;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import systems.project.configuratons.StorageProperties;
import systems.project.exceptions.StorageException;
import systems.project.models.storage.PreparedObject;
import systems.project.models.storage.StoredObject;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class MinioStorageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final MinioClient minioClient;
    private final StorageProperties properties;
    private final Logger logger;

    public MinioStorageService(MinioClient minioClient,
                               StorageProperties properties,
                               Logger logger) {
        this.minioClient = minioClient;
        this.properties = properties;
        this.logger = logger;
    }

    public PreparedObject prepareUpload(String originalFilename, byte[] content, String contentType) {
        String sanitized = sanitizeFilename(originalFilename);
        String folder = properties.resolveFolder();
        String date = DATE_FORMATTER.format(LocalDate.now());
        String baseKey = folder + "/" + date + "/" + UUID.randomUUID();
        String finalKey = baseKey + "-" + sanitized;
        String tempKey = finalKey + ".tmp";
        String mime = contentType == null || contentType.isBlank() ? MediaType.APPLICATION_JSON_VALUE : contentType;

        try (ByteArrayInputStream stream = new ByteArrayInputStream(content)) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(tempKey)
                    .contentType(mime)
                    .stream(stream, content.length, -1)
                    .build());
        } catch (Exception e) {
            throw new StorageException("Не удалось сохранить файл импорта во временном хранилище", e);
        }

        return new PreparedObject(minioClient, properties.getBucket(), tempKey, finalKey, content.length, mime, logger);
    }

    public StoredObject load(String storageKey) {
        try {
            return new StoredObject(minioClient.getObject(GetObjectArgs.builder()
                    .bucket(properties.getBucket())
                    .object(storageKey)
                    .build()));
        } catch (Exception e) {
            throw new StorageException("Файл импорта недоступен", e);
        }
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "import.json";
        }
        String normalized = filename.trim()
                .replaceAll("\\s+", "_")
                .replaceAll("[^A-Za-z0-9_\\.-]", "")
                .toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? "import.json" : normalized;
    }

}
