package systems.project.services.imports;

import org.springframework.stereotype.Component;
import systems.project.models.ImportFileLog;
import systems.project.models.api.ImportLogEntry;

@Component
public class ImportLogMapper {

    public ImportLogEntry toDto(ImportFileLog entity) {
        return ImportLogEntry.builder()
                .id(entity.getId())
                .filename(entity.getOriginalFilename())
                .storageKey(entity.getStorageKey())
                .contentType(entity.getContentType())
                .size(entity.getSize())
                .status(entity.getStatus())
                .requested(entity.getRequested())
                .imported(entity.getImported())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .completedAt(entity.getCompletedAt())
                .ticketIds(entity.getTicketIds() == null ? null : entity.getTicketIds().stream().toList())
                .downloadPath("/import/logs/" + entity.getId() + "/file")
                .build();
    }
}
