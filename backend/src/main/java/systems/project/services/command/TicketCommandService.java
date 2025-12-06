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
import systems.project.exceptions.InvalidDataException;
import systems.project.models.Coordinates;
import systems.project.models.Event;
import systems.project.models.Location;
import systems.project.models.Person;
import systems.project.models.Ticket;
import systems.project.models.TicketType;
import systems.project.models.Venue;
import systems.project.models.api.ImportResult;
import systems.project.repositories.EventRepository;
import systems.project.repositories.LocationRepository;
import systems.project.repositories.PersonRepository;
import systems.project.repositories.TicketRepository;
import systems.project.repositories.VenueRepository;
import systems.project.services.core.ValidateTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class TicketCommandService {

    private final TicketRepository ticketRepository;
    private final PersonRepository personRepository;
    private final ValidateTypes validateTypes;
    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final LocationRepository locationRepository;

    public TicketCommandService(TicketRepository ticketRepository,
                                PersonRepository personRepository,
                                ValidateTypes validateTypes,
                                EventRepository eventRepository,
                                VenueRepository venueRepository,
                                LocationRepository locationRepository) {
        this.ticketRepository = ticketRepository;
        this.personRepository = personRepository;
        this.validateTypes = validateTypes;
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.locationRepository = locationRepository;
    }

    @CacheStatsTracked
    public Map<String, List<Ticket>> getTickets() {
        try {
            List<Ticket> list = ticketRepository.findAllBy();
            return Map.of("tickets", list);
        } catch (Exception e) {
            return Collections.singletonMap("tickets", (List<Ticket>) null);
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Map<String, Boolean> addTicket(Ticket ticket) {
        try {
            if (!validatePerson(ticket.getPerson(), ticket.getEvent())) {
                return Map.of("status", false, "invalidPerson", true);
            }
            if (!validatePlace(ticket.getCoordinates(), ticket.getEvent())) {
                return Map.of("status", false, "invalidPlace", true);
            }
            if (validateTypes.compatibility(ticket)) {
                ticketRepository.save(ticket);
                return Map.of("status", true);
            }
            return Map.of("status", false, "invalidCompatibility", true);
        } catch (RuntimeException ex) {
            throwIfSerializationConflict(ex);
            return Map.of("status", false);
        }
    }

    @CacheStatsTracked
    public Ticket getTicket(Integer id) {
        try {
            return ticketRepository.findById(id).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean updateTicket(Integer id, Ticket ticket) {
        try {
            if (!ticketRepository.existsById(id)) {
                return false;
            }
            ticket.setId(id);
            ticketRepository.save(ticket);
            return true;
        } catch (RuntimeException ex) {
            throwIfSerializationConflict(ex);
            return false;
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean removeTicket(Integer id) {
        try {
            if (!ticketRepository.existsById(id)) {
                return false;
            }
            ticketRepository.deleteById(id);
            return true;
        } catch (RuntimeException ex) {
            throwIfSerializationConflict(ex);
            return false;
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean deleteAllByComment(String comment) {
        try {
            String c = comment == null ? "" : comment.trim();
            if (c.isEmpty()) {
                return false;
            }
            Long removed = ticketRepository.deleteByComment(c);
            return removed != null && removed > 0;
        } catch (RuntimeException ex) {
            throwIfSerializationConflict(ex);
            return false;
        }
    }

    @CacheStatsTracked
    public Ticket getWithMinEvent() {
        try {
            Optional<Ticket> res = ticketRepository.findFirstByEventIsNotNullOrderByEventIdAsc();
            return res.orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @CacheStatsTracked
    public Map<String, Long> countByCommentLess(String comment) {
        try {
            Long value = ticketRepository.countByCommentLessThan(comment);
            long count = value == null ? 0L : value;
            return Map.of("count", count);
        } catch (Exception e) {
            return Map.of("count", 0L);
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public boolean sellTicket(Integer ticketId, Integer personId, float amount) {
        if (amount <= 0f) {
            return false;
        }
        try {
            Optional<Ticket> ticketOpt = ticketRepository.findById(ticketId);
            if (ticketOpt.isEmpty()) {
                return false;
            }
            Optional<Person> personOpt = personRepository.findById(Long.valueOf(personId));
            if (personOpt.isEmpty()) {
                return false;
            }
            Ticket ticket = ticketOpt.get();
            ticket.setPrice(amount);
            ticket.setPerson(personOpt.get());
            ticketRepository.save(ticket);
            return true;
        } catch (RuntimeException ex) {
            throwIfSerializationConflict(ex);
            return false;
        }
    }

    @Retryable(
            retryFor = {CannotSerializeTransactionException.class, TransientDataAccessException.class},
            maxAttempts = 5,
            backoff = @Backoff(delay = 100, multiplier = 2, maxDelay = 1000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Ticket cloneVip(Integer ticketId) {
        try {
            Optional<Ticket> srcOpt = ticketRepository.findById(ticketId);
            if (srcOpt.isEmpty()) {
                return null;
            }
            Ticket src = srcOpt.get();
            Ticket copy = new Ticket();
            copy.setId(null);
            copy.setName(src.getName());
            copy.setCreationDate(LocalDateTime.now());
            copy.setPerson(src.getPerson());
            copy.setEvent(src.getEvent());
            copy.setVenue(src.getVenue());
            copy.setComment(src.getComment());
            copy.setNumber(src.getNumber());
            copy.setDiscount(src.getDiscount());
            copy.setType(TicketType.VIP);
            copy.setPrice(src.getPrice() * 2.0f);

            if (src.getCoordinates() != null) {
                Coordinates source = src.getCoordinates();
                Coordinates cloned = new Coordinates();
                cloned.setX(source.getX());
                cloned.setY(source.getY());
                copy.setCoordinates(cloned);
            }

            return ticketRepository.save(copy);
        } catch (RuntimeException ex) {
            throwIfSerializationConflict(ex);
            return null;
        }
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    @CacheStatsTracked
    public ImportResult importTickets(List<Ticket> tickets) throws InvalidDataException {
        if (tickets == null || tickets.isEmpty()) {
            throw new InvalidDataException("Список ticket пуст");
        }

        List<Integer> storedIds = new ArrayList<>();

        for (int i = 0; i < tickets.size(); i++) {
            int position = i + 1;
            Ticket prepared = prepareTicketForImport(tickets.get(i), position);
            try {
                Ticket saved = ticketRepository.save(prepared);
                storedIds.add(saved.getId());
            } catch (Exception e) {
                throw new InvalidDataException("Запись #" + position + ": не удалось сохранить: " + e.getMessage());
            }
        }

        return ImportResult.builder()
                .requested(tickets.size())
                .imported(storedIds.size())
                .ticketIds(storedIds)
                .build();
    }

    private Ticket prepareTicketForImport(Ticket source, int position) throws InvalidDataException {
        if (source == null) {
            throw new InvalidDataException("Запись #" + position + ": ticket отсутствует");
        }

        Ticket target = new Ticket();
        target.setId(null);
        target.setName(requireNonBlank(source.getName(), "ticket.name", position));
        target.setPrice(requirePositiveFloat(source.getPrice(), "ticket.price", position));
        target.setType(requireNotNull(source.getType(), "ticket.type", position));
        target.setNumber(requirePositiveInt(source.getNumber(), "ticket.number", position));
        target.setDiscount(source.getDiscount());
        target.setComment(trimToNull(source.getComment()));

        Coordinates coords = requireNotNull(source.getCoordinates(), "ticket.coordinates", position);
        Coordinates copyCoords = new Coordinates();
        copyCoords.setId(null);
        copyCoords.setX(coords.getX());
        copyCoords.setY(requireFloat(coords.getY(), "ticket.coordinates.y", position));
        target.setCoordinates(copyCoords);

        target.setPerson(resolvePerson(source.getPerson(), position));
        target.setEvent(resolveEvent(source.getEvent(), position));
        target.setVenue(resolveVenue(source.getVenue(), position));

        if (!validatePerson(target.getPerson(), target.getEvent())) {
            throw new InvalidDataException("Запись #" + position + ": некорректный владелец билета");
        }
        if (!validatePlace(target.getCoordinates(), target.getEvent())) {
            throw new InvalidDataException("Запись #" + position + ": координаты заняты для события");
        }
        if (!validateTypes.compatibility(target)) {
            throw new InvalidDataException("Запись #" + position + ": ticket type не совместим с зависимостями");
        }

        return target;
    }

    private Person resolvePerson(Person source, int position) throws InvalidDataException {
        if (source == null) {
            return null;
        }

        if (source.getId() != null) {
            int personId = Math.toIntExact(source.getId());
            return personRepository.findById((long) personId)
                    .orElseThrow(() -> new InvalidDataException(
                            "Запись #" + position + ": person с id=" + source.getId() + " не найден"));
        }

        String passport = requireNonBlank(source.getPassportID(), "person.passportID", position);
        if (personRepository.existsPersonByPassportID(passport)) {
            throw new InvalidDataException(
                    "Запись #" + position + ": person с passportID=" + passport + " уже существует");
        }

        Location location = requireNotNull(source.getLocation(), "person.location", position);
        Location storedLocation = locationRepository.save(Location.builder()
                .id(null)
                .x(requireNotNull(location.getX(), "person.location.x", position))
                .y(requireFloat(location.getY(), "person.location.y", position))
                .z(requireNotNull(location.getZ(), "person.location.z", position))
                .build());

        Person person = new Person();
        person.setId(null);
        person.setPassportID(passport);
        person.setWeight(requirePositiveDouble(source.getWeight(), "person.weight", position));
        person.setNationality(requireNotNull(source.getNationality(), "person.nationality", position));
        person.setHairColor(requireNotNull(source.getHairColor(), "person.hairColor", position));
        person.setEyeColor(source.getEyeColor());
        person.setLocation(storedLocation);

        return personRepository.save(person);
    }

    private Event resolveEvent(Event source, int position) throws InvalidDataException {
        if (source == null) {
            return null;
        }

        if (source.getId() != null) {
            return eventRepository.findById(source.getId())
                    .orElseThrow(() -> new InvalidDataException(
                            "Запись #" + position + ": event с id=" + source.getId() + " не найден"));
        }

        Event event = new Event();
        event.setId(null);
        event.setName(requireNonBlank(source.getName(), "event.name", position));
        event.setTicketsCount(requirePositiveInt(source.getTicketsCount(), "event.ticketsCount", position));
        event.setEventType(source.getEventType());

        return eventRepository.save(event);
    }

    private Venue resolveVenue(Venue source, int position) throws InvalidDataException {
        if (source == null) {
            return null;
        }

        if (source.getId() != null) {
            return venueRepository.findById(source.getId())
                    .orElseThrow(() -> new InvalidDataException(
                            "Запись #" + position + ": venue с id=" + source.getId() + " не найден"));
        }

        Venue venue = new Venue();
        venue.setId(null);
        venue.setName(requireNonBlank(source.getName(), "venue.name", position));
        venue.setCapacity(requirePositiveInt(source.getCapacity(), "venue.capacity", position));
        venue.setType(source.getType());

        return venueRepository.save(venue);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireNonBlank(String value, String field, int position) throws InvalidDataException {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidDataException("Запись #" + position + ": " + field + " обязательно");
        }
        return value.trim();
    }

    private <T> T requireNotNull(T value, String field, int position) throws InvalidDataException {
        if (value == null) {
            throw new InvalidDataException("Запись #" + position + ": " + field + " обязательно");
        }
        return value;
    }

    private int requirePositiveInt(Number value, String field, int position) throws InvalidDataException {
        if (value == null || value.intValue() <= 0) {
            throw new InvalidDataException("Запись #" + position + ": " + field + " должно быть > 0");
        }
        return value.intValue();
    }

    private float requirePositiveFloat(Number value, String field, int position) throws InvalidDataException {
        if (value == null || value.floatValue() <= 0f) {
            throw new InvalidDataException("Запись #" + position + ": " + field + " должно быть > 0");
        }
        return value.floatValue();
    }

    private float requireFloat(Number value, String field, int position) throws InvalidDataException {
        if (value == null) {
            throw new InvalidDataException("Запись #" + position + ": " + field + " обязательно");
        }
        return value.floatValue();
    }

    private double requirePositiveDouble(Number value, String field, int position) throws InvalidDataException {
        if (value == null || value.doubleValue() <= 0d) {
            throw new InvalidDataException("Запись #" + position + ": " + field + " должно быть > 0");
        }
        return value.doubleValue();
    }

    private boolean validatePerson(Person person, Event event) {
        if (person == null || event == null || person.getId() == null) {
            return true;
        }
        Optional<Person> personOptional = personRepository.findById(person.getId());
        if (personOptional.isEmpty()) {
            return false;
        }
        Person presented = personOptional.get();
        List<Ticket> tickets = presented.getTickets();
        if (tickets == null || tickets.size() < 10) {
            return true;
        }
        int count = 0;
        for (Ticket t : tickets) {
            if (t.getEvent() != null && t.getEvent().equals(event)) {
                count++;
            }
        }
        return count < 10;
    }

    private boolean validatePlace(Coordinates coordinates, Event event) {
        if (event == null || event.getId() == null) {
            return true;
        }
        Optional<Event> eventOptional = eventRepository.findById(event.getId());
        if (eventOptional.isEmpty()) {
            return true;
        }
        Event presented = eventOptional.get();
        List<Ticket> tickets = presented.getTickets();
        if (tickets == null || coordinates == null) {
            return true;
        }
        for (Ticket ticket : tickets) {
            if (coordinates.equals(ticket.getCoordinates())) {
                return false;
            }
        }
        return true;
    }

    private void throwIfSerializationConflict(RuntimeException ex) {
        if (ex instanceof TransientDataAccessException) {
            throw ex;
        }
        if (isSerializationConflict(ex)) {
            throw cannotSerialize(ex);
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

    private CannotSerializeTransactionException cannotSerialize(Throwable ex) {
        return new CannotSerializeTransactionException("Serialization conflict", ex);
    }

    @Recover
    public Map<String, Boolean> recoverMapResult(TransientDataAccessException ex, Object... args) {
        return Map.of("status", false);
    }

    @Recover
    public boolean recoverBooleanResult(TransientDataAccessException ex, Object... args) {
        return false;
    }

    @Recover
    public Ticket recoverTicketResult(TransientDataAccessException ex, Object... args) {
        return null;
    }
}
