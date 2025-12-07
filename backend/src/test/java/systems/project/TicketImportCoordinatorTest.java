package systems.project;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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
import systems.project.services.imports.TicketImportCoordinator;
import systems.project.services.imports.TicketImportParser;
import systems.project.services.storage.MinioStorageService;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketImportCoordinatorTest {

    @Mock
    private TicketCommandService commandService;
    @Mock
    private TicketImportParser parser;
    @Mock
    private MinioStorageService storageService;
    @Mock
    private ImportFileLogRepository logRepository;
    @Mock
    private MultipartFile multipartFile;
    @Mock
    private PreparedObject preparedObject;

    @Captor
    private ArgumentCaptor<ImportFileLog> logCaptor;

    @InjectMocks
    private TicketImportCoordinator coordinator;

    @BeforeEach
    void initSync() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void importFromFileCommitsPreparedObjectOnSuccess() throws Exception {
        String payload = "[{\"name\":\"A\",\"price\":1,\"number\":1,\"type\":\"VIP\"," +
                "\"coordinates\":{\"x\":1,\"y\":1.0}}]";
        byte[] bytes = payload.getBytes();
        when(multipartFile.getBytes()).thenReturn(bytes);
        when(multipartFile.getOriginalFilename()).thenReturn("tickets.json");
        when(multipartFile.getContentType()).thenReturn("application/json");
        List<Ticket> parsed = List.of(new Ticket());
        when(parser.parse(bytes)).thenReturn(parsed);
        when(storageService.prepareUpload(eq("tickets.json"), eq(bytes), eq("application/json")))
                .thenReturn(preparedObject);
        when(preparedObject.getFinalKey()).thenReturn("imports/2025/01/tickets.json");
        when(preparedObject.getContentType()).thenReturn("application/json");
        when(preparedObject.getSize()).thenReturn((long) bytes.length);
        when(logRepository.save(any())).thenAnswer(invocation -> {
            ImportFileLog log = invocation.getArgument(0);
            if (log.getId() == null) {
                log.setId(123L);
            }
            return log;
        });
        ImportResult commandResult = ImportResult.builder()
                .requested(1)
                .imported(1)
                .ticketIds(List.of(42))
                .build();
        when(commandService.importTickets(parsed)).thenReturn(commandResult);

        ImportResult result = coordinator.importFromFile(multipartFile);

        TransactionSynchronization sync = TransactionSynchronizationManager.getSynchronizations().get(0);
        sync.beforeCommit(false);
        sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);

        verify(preparedObject).commit();
        verify(preparedObject, never()).rollback();
        verify(logRepository, atLeast(2)).save(logCaptor.capture());
        ImportFileLog finalLog = logCaptor.getAllValues().get(logCaptor.getAllValues().size() - 1);
        assertThat(finalLog.getStatus()).isEqualTo(ImportStatus.SUCCESS);
        assertThat(finalLog.getImported()).isEqualTo(1);
        assertThat(finalLog.getTicketIds()).containsExactly(42);
        assertThat(result.getLogId()).isEqualTo(123L);
        assertThat(result.getStorageKey()).isEqualTo("imports/2025/01/tickets.json");
    }

    @Test
    void importFromFileRollsBackOnInvalidData() throws Exception {
        byte[] bytes = "[]".getBytes();
        when(multipartFile.getBytes()).thenReturn(bytes);
        when(multipartFile.getOriginalFilename()).thenReturn("broken.json");
        when(multipartFile.getContentType()).thenReturn("application/json");
        when(parser.parse(bytes)).thenReturn(Collections.singletonList(new Ticket()));
        when(storageService.prepareUpload(eq("broken.json"), eq(bytes), eq("application/json")))
                .thenReturn(preparedObject);
        when(preparedObject.getFinalKey()).thenReturn("imports/tmp/broken.json");
        when(preparedObject.getContentType()).thenReturn("application/json");
        when(preparedObject.getSize()).thenReturn(0L);
        when(logRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(commandService.importTickets(any())).thenThrow(new InvalidDataException("boom"));

        assertThrows(InvalidDataException.class, () -> coordinator.importFromFile(multipartFile));

        TransactionSynchronization sync = TransactionSynchronizationManager.getSynchronizations().get(0);
        sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(preparedObject, never()).commit();
        verify(preparedObject).rollback();
        verify(logRepository, atLeast(2)).save(logCaptor.capture());
        ImportFileLog failedLog = logCaptor.getAllValues().get(logCaptor.getAllValues().size() - 1);
        assertThat(failedLog.getStatus()).isEqualTo(ImportStatus.FAILED);
        assertThat(failedLog.getErrorMessage()).contains("boom");
    }

    @Test
    void importFromFileRollsBackOnRuntimeException() throws Exception {
        byte[] bytes = "[]".getBytes();
        when(multipartFile.getBytes()).thenReturn(bytes);
        when(multipartFile.getOriginalFilename()).thenReturn("runtime.json");
        when(multipartFile.getContentType()).thenReturn("application/json");
        when(parser.parse(bytes)).thenReturn(Collections.singletonList(new Ticket()));
        when(storageService.prepareUpload(eq("runtime.json"), eq(bytes), eq("application/json")))
                .thenReturn(preparedObject);
        when(preparedObject.getFinalKey()).thenReturn("imports/tmp/runtime.json");
        when(preparedObject.getContentType()).thenReturn("application/json");
        when(preparedObject.getSize()).thenReturn(0L);
        when(logRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(commandService.importTickets(any())).thenThrow(new RuntimeException("unexpected"));

        assertThrows(RuntimeException.class, () -> coordinator.importFromFile(multipartFile));

        TransactionSynchronization sync = TransactionSynchronizationManager.getSynchronizations().get(0);
        sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);

        verify(preparedObject, never()).commit();
        verify(preparedObject, atLeastOnce()).rollback();
        verify(logRepository, atLeast(2)).save(logCaptor.capture());
        ImportFileLog failedLog = logCaptor.getAllValues().get(logCaptor.getAllValues().size() - 1);
        assertThat(failedLog.getStatus()).isEqualTo(ImportStatus.FAILED);
        assertThat(failedLog.getErrorMessage()).contains("unexpected");
    }
}
