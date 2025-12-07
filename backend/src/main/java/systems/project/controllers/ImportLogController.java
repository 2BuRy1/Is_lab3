package systems.project.controllers;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import systems.project.exceptions.ResourceNotFoundException;
import systems.project.models.api.ImportLogEntry;
import systems.project.models.api.AbstractResponse;
import systems.project.models.storage.StoredObject;
import systems.project.services.imports.ImportLogService;
import systems.project.services.storage.MinioStorageService;

import java.io.InputStream;
import java.util.List;

@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/import/logs")
public class ImportLogController {

    private final ImportLogService logService;
    private final MinioStorageService storageService;

    public ImportLogController(ImportLogService logService,
                               MinioStorageService storageService) {
        this.logService = logService;
        this.storageService = storageService;
    }

    @GetMapping
    public ResponseEntity<AbstractResponse<List<ImportLogEntry>>> list() {
        List<ImportLogEntry> entries = logService.list();
        return ResponseEntity.ok(AbstractResponse.<List<ImportLogEntry>>builder()
                .status("ok")
                .title("Журнал импорта")
                .message("Всего записей: " + entries.size())
                .data(entries)
                .build());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AbstractResponse<ImportLogEntry>> get(@PathVariable Long id) {
        ImportLogEntry entry = logService.get(id);
        return ResponseEntity.ok(AbstractResponse.<ImportLogEntry>builder()
                .status("ok")
                .title("Запись импорта")
                .message("Детали импорта")
                .data(entry)
                .build());
    }

    @GetMapping(value = "/{id}/file", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<StreamingResponseBody> download(@PathVariable Long id) {
        ImportLogEntry entry = logService.get(id);
        if (entry.getStorageKey() == null) {
            throw new ResourceNotFoundException("Файл для записи импорта недоступен");
        }
        StreamingResponseBody body = outputStream -> {
            try (StoredObject resource = storageService.load(entry.getStorageKey());
                 InputStream stream = resource.stream()) {
                stream.transferTo(outputStream);
            }
        };

        String filename = entry.getFilename() == null ? "import.json" : entry.getFilename();
        String contentType = entry.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : entry.getContentType();

        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(contentType));
        if (entry.getSize() != null) {
            builder.header(HttpHeaders.CONTENT_LENGTH, entry.getSize().toString());
        }
        return builder.body(body);
    }
}
