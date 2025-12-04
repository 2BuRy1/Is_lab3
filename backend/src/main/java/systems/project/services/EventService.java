package systems.project.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import systems.project.models.Event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Service
public class EventService {

    private final EventCommandService commands;

    public EventService(EventCommandService commands) {
        this.commands = commands;
    }

    @Async
    public CompletableFuture<Map<String, List<Event>>> getEvents() {
        return completedFuture(commands.getEvents());
    }

    @Async
    public CompletableFuture<Map<String, Boolean>> addEvent(Event event) {
        return completedFuture(commands.addEvent(event));
    }
}
