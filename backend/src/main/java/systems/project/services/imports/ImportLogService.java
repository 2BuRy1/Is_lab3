package systems.project.services.imports;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import systems.project.exceptions.ResourceNotFoundException;
import systems.project.models.api.ImportLogEntry;
import systems.project.repositories.ImportFileLogRepository;
import systems.project.models.ImportFileLog;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ImportLogService {

    private final ImportFileLogRepository repository;
    private final ImportLogMapper mapper;

    public ImportLogService(ImportFileLogRepository repository,
                            ImportLogMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<ImportLogEntry> list() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    public ImportLogEntry get(Long id) {
        ImportFileLog entity = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Запись импорта не найдена"));
        return mapper.toDto(entity);
    }
}
