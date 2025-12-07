package systems.project.services.imports;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import systems.project.exceptions.InvalidDataException;
import systems.project.models.ImportFileLog;
import systems.project.models.ImportStatus;
import systems.project.models.Ticket;
import systems.project.models.api.ImportResult;
import systems.project.models.storage.PreparedObject;
import systems.project.repositories.ImportFileLogRepository;
import systems.project.services.command.TicketCommandService;
import systems.project.services.storage.MinioStorageService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketImportCoordinator {

    private final TicketCommandService ticketCommandService;
    private final TicketImportParser parser;
    private final MinioStorageService storageService;
    private final ImportFileLogRepository logRepository;

    public TicketImportCoordinator(TicketCommandService ticketCommandService,
                                   TicketImportParser parser,
                                   MinioStorageService storageService,
                                   ImportFileLogRepository logRepository) {
        this.ticketCommandService = ticketCommandService;
        this.parser = parser;
        this.storageService = storageService;
        this.logRepository = logRepository;
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public ImportResult importFromFile(MultipartFile multipartFile) throws InvalidDataException {
        byte[] bytes = readBytes(multipartFile);
        List<Ticket> tickets = parser.parse(bytes);
        if (tickets.isEmpty()) {
            throw new InvalidDataException("Файл не содержит записей ticket");
        }
        PreparedObject preparedObject = storageService.prepareUpload(
                multipartFile.getOriginalFilename(),
                bytes,
                multipartFile.getContentType());

        ImportFileLog logEntry = ImportFileLog.builder()
                .originalFilename(multipartFile.getOriginalFilename())
                .storageKey(preparedObject.getFinalKey())
                .contentType(preparedObject.getContentType())
                .size(preparedObject.getSize())
                .status(ImportStatus.PENDING)
                .requested(tickets.size())
                .createdAt(LocalDateTime.now())
                .build();
        logRepository.save(logEntry);

        registerSynchronization(preparedObject);

        try {
            ImportResult result = ticketCommandService.importTickets(tickets);
            logEntry.setImported(result.getImported());
            List<Integer> ids = result.getTicketIds() == null
                    ? new ArrayList<>()
                    : new ArrayList<>(result.getTicketIds());
            logEntry.setTicketIds(ids);
            logEntry.setStatus(ImportStatus.SUCCESS);
            logEntry.setCompletedAt(LocalDateTime.now());
            logRepository.save(logEntry);

            result.setLogId(logEntry.getId());
            result.setStorageKey(logEntry.getStorageKey());
            result.setFilename(logEntry.getOriginalFilename());
            return result;
        } catch (InvalidDataException ex) {
            markFailed(logEntry, ex.getMessage());
            throw ex;
        } catch (RuntimeException ex) {
            markFailed(logEntry, ex.getMessage());
            throw ex;
        }
    }

    private void markFailed(ImportFileLog logEntry, String message) {
        logEntry.setStatus(ImportStatus.FAILED);
        logEntry.setErrorMessage(message);
        logEntry.setCompletedAt(LocalDateTime.now());
        logRepository.save(logEntry);
    }

    private byte[] readBytes(MultipartFile multipartFile) throws InvalidDataException {
        try {
            byte[] bytes = multipartFile.getBytes();
            if (bytes.length == 0) {
                throw new InvalidDataException("Файл пуст");
            }
            return bytes;
        } catch (IOException e) {
            throw new InvalidDataException("Не удалось прочитать файл", e);
        }
    }

    private void registerSynchronization(PreparedObject prepared) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void beforeCommit(boolean readOnly) {
                prepared.commit();
            }

            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    prepared.rollback();
                }
            }
        });
    }
}
