package systems.project.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import systems.project.exceptions.ResourceNotFoundException;
import systems.project.exceptions.StorageException;
import systems.project.models.api.AbstractResponse;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<AbstractResponse<Void>> handleNotFound(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(AbstractResponse.<Void>builder()
                        .status("error")
                        .title("Не найдено")
                        .message(ex.getMessage())
                        .data(null)
                        .build());
    }

    @ExceptionHandler(StorageException.class)
    public ResponseEntity<AbstractResponse<Void>> handleStorage(StorageException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(AbstractResponse.<Void>builder()
                        .status("error")
                        .title("Ошибка хранилища")
                        .message(ex.getMessage())
                        .data(null)
                        .build());
    }
}
