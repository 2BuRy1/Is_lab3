// TicketEventsService.java
package systems.project.services;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

@Service
public class TicketEventService {
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final Logger logger;

    public TicketEventService(Logger logger) {
        this.logger = logger;
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> {
            emitters.remove(emitter);
            emitter.complete();
        });
        emitter.onError(ex -> {
            emitters.remove(emitter);
            try {
                emitter.complete();
            } catch (Exception ignored) { }
        });
        try {
            emitter.send(SseEmitter.event()
                    .data("connected")
                    .reconnectTime(3000)
                    .id(String.valueOf(System.currentTimeMillis()))
                    .build());
        } catch (IOException ignored) { }

        return emitter;
    }

    public void publishChange(String action, Integer id) {
        logger.info("published");
        emitters.forEach(em -> {
            try {
                em.send(SseEmitter.event()
                        .data("{\"action\":\"" + action + "\",\"id\":" + id + "}")
                        .id(String.valueOf(System.currentTimeMillis()))
                        .build());
            } catch (IOException e) {
                em.complete();
                emitters.remove(em);
            }
        });
    }
}