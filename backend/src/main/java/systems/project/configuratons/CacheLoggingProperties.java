package systems.project.configuratons;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.cache")
@Getter
@Setter
public class CacheLoggingProperties {

    /**
     * Controls whether the L2 cache statistics aspect prints hit/miss information.
     */
    private boolean loggingEnabled = false;
}
