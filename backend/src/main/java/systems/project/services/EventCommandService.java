package systems.project.services;

import org.postgresql.util.PSQLException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import systems.project.cache.CacheStatsTracked;
import systems.project.models.Event;
import systems.project.repositories.EventRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class EventCommandService {

    private final EventRepository eventRepository;

    public EventCommandService(EventRepository eventRepository) {
        this.eventRepository = eventRepository;
    }

    @CacheStatsTracked
    public Map<String, List<Event>> getEvents() {
        try {
            List<Event> events = eventRepository.findAllBy();
            return Map.of("events", events);
        } catch (Exception e) {
            return Collections.singletonMap("events", (List<Event>) null);
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Map<String, Boolean> addEvent(Event event) {
        try {
            eventRepository.save(event);
            return Map.of("status", true);
        } catch (RuntimeException ex) {
            throwIfSerializationConflict(ex);
            return Map.of("status", false);
        }
    }

    @Recover
    public Map<String, Boolean> recoverAddEvent(TransientDataAccessException ex, Event event) {
        return Map.of("status", false);
    }

    private void throwIfSerializationConflict(RuntimeException ex) {
        if (ex instanceof TransientDataAccessException) {
            throw ex;
        }
        if (isSerializationConflict(ex)) {
            throw new CannotSerializeTransactionException("Serialization conflict", ex);
        }
    }

    private boolean isSerializationConflict(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof CannotSerializeTransactionException) {
                return true;
            }
            if (current instanceof PSQLException psql && "40001".equals(psql.getSQLState())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
