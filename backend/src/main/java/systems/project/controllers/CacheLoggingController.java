package systems.project.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import systems.project.configuratons.CacheLoggingProperties;
import systems.project.models.api.CacheLoggingRequest;

import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/cache/l2/logging")
public class CacheLoggingController {

    private final CacheLoggingProperties properties;

    public CacheLoggingController(CacheLoggingProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> status() {
        return ResponseEntity.ok(Map.of(
                "enabled", properties.isLoggingEnabled()
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> update(@RequestBody CacheLoggingRequest request) {
        properties.setLoggingEnabled(request.isEnabled());
        return ResponseEntity.ok(Map.of(
                "enabled", properties.isLoggingEnabled()
        ));
    }
}
