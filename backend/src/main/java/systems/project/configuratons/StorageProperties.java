package systems.project.configuratons;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.storage.minio")
public class StorageProperties {

    private String endpoint;
    private String bucket;
    private String accessKey;
    private String secretKey;
    private String region;
    private boolean secure;
    private String folder = "imports";

    public String resolveFolder() {
        if (folder == null || folder.isBlank()) {
            return "imports";
        }
        return folder.replaceAll("^/+", "").replaceAll("/+", "/");
    }
}
