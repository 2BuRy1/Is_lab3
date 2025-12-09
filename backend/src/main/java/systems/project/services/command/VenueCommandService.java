package systems.project.services.command;

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
import systems.project.models.Venue;
import systems.project.repositories.VenueRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class VenueCommandService {

    private final VenueRepository venueRepository;

    public VenueCommandService(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @CacheStatsTracked
    public Map<String, List<Venue>> getVenues() {
        try {
            List<Venue> venues = venueRepository.findAllBy();
            return Map.of("venues", venues);
        } catch (Exception e) {
            return Collections.singletonMap("venues", (List<Venue>) null);
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Map<String, Boolean> addVenue(Venue venue) {
        try {
            venueRepository.save(venue);
            return Map.of("status", true);
        } catch (RuntimeException ex) {
            throwIfSerializationConflict(ex);
            return Map.of("status", false);
        }
    }

    @Recover
    public Map<String, Boolean> recoverAddVenue(TransientDataAccessException ex, Venue venue) {
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
