package systems.project.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import systems.project.controllers.api.EventsApi;
import systems.project.models.Event;
import systems.project.models.api.AbstractResponse;
import systems.project.models.envelopes.EventsEnvelope;
import systems.project.services.EventService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = {"*"})
public class EventsApiController implements EventsApi {

    private final EventService service;

    public EventsApiController(EventService service) {
        this.service = service;
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Event>>> addEvent(Event event) {
        return service.addEvent(event)
                .thenApply(res -> {
                    boolean ok = res != null && Boolean.TRUE.equals(res.get("status"));
                    if (ok) {
                        return ResponseEntity.ok(
                                AbstractResponse.<Event>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Событие создано")
                                        .data(event)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Event>builder()
                                    .status("error")
                                    .title("Ошибка")
                                    .message("Ошибка при создании события")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Event>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<EventsEnvelope>>> getEvents() {
        return service.getEvents()
                .thenApply(map -> {
                    List<Event> events = map == null ? null : map.get("events");
                    EventsEnvelope envelope = new EventsEnvelope();
                    envelope.setEventList(events);

                    return ResponseEntity.ok(
                            AbstractResponse.<EventsEnvelope>builder()
                                    .status("ok")
                                    .title("Успех")
                                    .message("Список событий")
                                    .data(envelope)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<EventsEnvelope>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }
}
