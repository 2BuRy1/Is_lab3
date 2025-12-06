package systems.project.models.api;

import lombok.Builder;
import lombok.Data;
import systems.project.models.ImportStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ImportLogEntry {
    private Long id;
    private String filename;
    private String storageKey;
    private String contentType;
    private Long size;
    private ImportStatus status;
    private Integer requested;
    private Integer imported;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
    private List<Integer> ticketIds;
    private String downloadPath;
}
