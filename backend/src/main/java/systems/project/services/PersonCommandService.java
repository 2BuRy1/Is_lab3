package systems.project.services;

import lombok.extern.slf4j.Slf4j;
import org.postgresql.util.PSQLException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.CannotSerializeTransactionException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import systems.project.models.Person;
import systems.project.repositories.LocationRepository;
import systems.project.repositories.PersonRepository;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@Slf4j
public class PersonCommandService {

    private final PersonRepository personRepository;
    private final LocationRepository locationRepository;

    public PersonCommandService(PersonRepository personRepository,
                                LocationRepository locationRepository) {
        this.personRepository = personRepository;
        this.locationRepository = locationRepository;
    }

    public Map<String, List<Person>> getPersons() {
        try {
            List<Person> people = personRepository.findAllBy();
            return Map.of("persons", people);
        } catch (Exception e) {
            return Collections.singletonMap("persons", (List<Person>) null);
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Map<String, Boolean> addPerson(Person person) {
        try {
            log.info("enterd to create person");
            if (person == null || person.getPassportID() == null) {
                log.info("Person payload is null or missing passportId");
                return Map.of("status", false, "passportId", true);
            }
            if (personRepository.existsPersonByPassportID(person.getPassportID())) {
                return Map.of("status", false, "passportId", true);
            }
            var savedLocation = locationRepository.save(person.getLocation());
            person.setLocation(savedLocation);
            personRepository.save(person);
            return Map.of("status", true);
        } catch (RuntimeException ex) {
            if (isDuplicatePassport(ex)) {
                return Map.of("status", false, "passportId", true);
            }
            throwIfSerializationConflict(ex);
            return Map.of("status", false);
        }
    }

    @Recover
    public Map<String, Boolean> recoverAddPerson(TransientDataAccessException ex, Person person) {
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

    private boolean isDuplicatePassport(Throwable throwable) {
        ConstraintViolationException violation = findConstraintViolation(throwable);
        if (violation == null) {
            return false;
        }
        String constraint = violation.getConstraintName();
        if (constraint != null && constraint.toLowerCase().contains("passport")) {
            return true;
        }
        String message = violation.getSQLException() != null ?
                violation.getSQLException().getMessage() :
                violation.getMessage();
        return message != null && message.toLowerCase().contains("passport");
    }

    private ConstraintViolationException findConstraintViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException violation) {
                return violation;
            }
            if (current instanceof DataIntegrityViolationException dive &&
                    dive.getCause() instanceof
                            ConstraintViolationException violation) {
                return violation;
            }
            current = current.getCause();
        }
        return null;
    }
}
