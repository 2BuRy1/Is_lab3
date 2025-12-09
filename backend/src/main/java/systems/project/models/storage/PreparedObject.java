package systems.project.models.storage;

import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import systems.project.exceptions.StorageException;

import java.util.logging.Logger;

public class PreparedObject {

    private final MinioClient client;
    private final String bucket;
    private final String tempKey;
    private final String finalKey;
    private final long size;
    private final String contentType;
    private final Logger logger;
    private boolean promoted;

    public PreparedObject(MinioClient client,
                          String bucket,
                          String tempKey,
                          String finalKey,
                          long size,
                          String contentType,
                          Logger logger) {
        this.client = client;
        this.bucket = bucket;
        this.tempKey = tempKey;
        this.finalKey = finalKey;
        this.size = size;
        this.contentType = contentType;
        this.logger = logger;
    }

    public void commit() {
        try {
            client.copyObject(CopyObjectArgs.builder()
                    .bucket(bucket)
                    .object(finalKey)
                    .source(CopySource.builder().bucket(bucket).object(tempKey).build())
                    .build());
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(tempKey)
                    .build());
            promoted = true;
        } catch (Exception e) {
            throw new StorageException("Не удалось зафиксировать файл импорта", e);
        }
    }

    public void rollback() {
        try {
            String target = promoted ? finalKey : tempKey;
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(target)
                    .build());
        } catch (Exception e) {
            logger.warning("Не удалось удалить временный объект MinIO: " + e.getMessage());
        }
    }

    public String getFinalKey() {
        return finalKey;
    }

    public long getSize() {
        return size;
    }

    public String getContentType() {
        return contentType;
    }
}
