package systems.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import systems.project.models.Coordinates;
import systems.project.models.Event;
import systems.project.models.EventType;
import systems.project.models.Location;
import systems.project.models.Person;
import systems.project.models.Ticket;
import systems.project.models.TicketType;
import systems.project.models.Venue;
import systems.project.repositories.EventRepository;
import systems.project.repositories.LocationRepository;
import systems.project.repositories.PersonRepository;
import systems.project.repositories.TicketRepository;
import systems.project.repositories.VenueRepository;
import systems.project.services.EventCommandService;
import systems.project.services.EventService;
import systems.project.services.PersonCommandService;
import systems.project.services.PersonService;
import systems.project.services.TicketCommandService;
import systems.project.services.TicketService;
import systems.project.services.ValidateTypes;
import systems.project.services.VenueCommandService;
import systems.project.services.VenueService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAndSaveTests {

    @Mock
    TicketRepository ticketRepository;

    @Mock
    PersonRepository personRepository;

    @Mock
    EventRepository eventRepository;

    @Mock
    VenueRepository venueRepository;

    @Mock
    LocationRepository locationRepository;

    @Mock
    ValidateTypes validateTypes;

    private TicketCommandService ticketCommandService;
    private PersonCommandService personCommandService;
    private EventCommandService eventCommandService;
    private VenueCommandService venueCommandService;

    private TicketService ticketService;
    private PersonService personService;
    private EventService eventService;
    private VenueService venueService;

    @BeforeEach
    void setUp() {
        ticketCommandService = new TicketCommandService(
                ticketRepository,
                personRepository,
                validateTypes,
                eventRepository,
                venueRepository,
                locationRepository
        );
        personCommandService = new PersonCommandService(personRepository, locationRepository);
        eventCommandService = new EventCommandService(eventRepository);
        venueCommandService = new VenueCommandService(venueRepository);

        ticketService = new TicketService(ticketCommandService);
        personService = new PersonService(personCommandService);
        eventService = new EventService(eventCommandService);
        venueService = new VenueService(venueCommandService);
    }

    @Test
    void testGetAllTickets() throws ExecutionException, InterruptedException {
        List<Ticket> tickets = List.of(new Ticket());
        when(ticketRepository.findAllBy()).thenReturn(tickets);

        var res = ticketService.getTickets().get();

        assertNotNull(res.get("tickets"));
        verify(ticketRepository).findAllBy();
    }

    @Test
    void testGetAllEvents() throws ExecutionException, InterruptedException {
        List<Event> events = List.of(new Event());
        when(eventRepository.findAllBy()).thenReturn(events);

        var res = eventService.getEvents().get();

        assertNotNull(res.get("events"));
        verify(eventRepository).findAllBy();
    }

    @Test
    void testGetAllPersons() throws ExecutionException, InterruptedException {
        List<Person> persons = List.of(new Person());
        when(personRepository.findAllBy()).thenReturn(persons);

        var res = personService.getPersons().get();

        assertNotNull(res.get("persons"));
        verify(personRepository).findAllBy();
    }

    @Test
    void testGetAllVenues() throws ExecutionException, InterruptedException {
        List<Venue> venues = List.of(new Venue());
        when(venueRepository.findAllBy()).thenReturn(venues);

        var res = venueService.getVenues().get();

        assertNotNull(res.get("venues"));
        verify(venueRepository).findAllBy();
    }

    @Test
    void testAddTicket() throws ExecutionException, InterruptedException {
        Ticket ticket = new Ticket();
        ticket.setName("Test Ticket");
        ticket.setType(TicketType.USUAL);

        when(validateTypes.compatibility(ticket)).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        var res = ticketService.addTicket(ticket).get();

        assertTrue(Boolean.TRUE.equals(res.get("status")));
        verify(ticketRepository).save(eq(ticket));
    }

    @Test
    void testAddEvent() throws ExecutionException, InterruptedException {
        Event event = new Event();
        event.setName("Concert");
        event.setTicketsCount(10);

        when(eventRepository.save(any(Event.class))).thenReturn(event);

        var res = eventService.addEvent(event).get();

        assertTrue(Boolean.TRUE.equals(res.get("status")));
        verify(eventRepository).save(eq(event));
    }

    @Test
    void testAddPerson() throws ExecutionException, InterruptedException {
        Location location = new Location();
        Person person = new Person();
        person.setLocation(location);
        person.setPassportID("passport");

        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(personRepository.existsPersonByPassportID("passport")).thenReturn(false);
        when(personRepository.save(any(Person.class))).thenReturn(person);

        var res = personService.addPerson(person).get();

        assertTrue(Boolean.TRUE.equals(res.get("status")));
        verify(locationRepository).save(eq(location));
        verify(personRepository).save(eq(person));
    }

    @Test
    void testAddVenue() throws ExecutionException, InterruptedException {
        Venue venue = new Venue();
        venue.setName("Main Hall");

        when(venueRepository.save(any(Venue.class))).thenReturn(venue);

        var res = venueService.addVenue(venue).get();

        assertTrue(Boolean.TRUE.equals(res.get("status")));
        verify(venueRepository).save(eq(venue));
    }

    @Test
    void testFailAddTicket() throws ExecutionException, InterruptedException {
        Ticket ticket = new Ticket();
        ticket.setName("Broken");
        ticket.setType(TicketType.VIP);

        when(validateTypes.compatibility(ticket)).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenThrow(new RuntimeException("failed"));

        var res = ticketService.addTicket(ticket).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
    }

    @Test
    void testFailAddTicketViaPersonValidation() throws ExecutionException, InterruptedException {
        Event event = new Event();
        event.setId(1);

        Person person = new Person();
        person.setId(1L);
        List<Ticket> owned = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            Ticket t = new Ticket();
            t.setEvent(event);
            owned.add(t);
        }
        person.setTickets(owned);

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setPerson(person);

        when(personRepository.findById(1L)).thenReturn(Optional.of(person));

        var res = ticketService.addTicket(ticket).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
        assertTrue(Boolean.TRUE.equals(res.get("invalidPerson")));
    }

    @Test
    void testFailAddTicketViaCoordinates() throws ExecutionException, InterruptedException {
        Coordinates conflict = new Coordinates();
        conflict.setX(5);
        conflict.setY(3f);

        Event event = new Event();
        event.setId(99);

        Ticket ticket = new Ticket();
        ticket.setEvent(event);
        ticket.setCoordinates(conflict);

        Event persisted = new Event();
        persisted.setId(99);
        Ticket existing = new Ticket();
        existing.setCoordinates(conflict);
        persisted.setTickets(List.of(existing));

        when(eventRepository.findById(99)).thenReturn(Optional.of(persisted));

        var res = ticketService.addTicket(ticket).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
        assertTrue(Boolean.TRUE.equals(res.get("invalidPlace")));
    }

    @Test
    void testFailAddTicketViaCompatibility() throws ExecutionException, InterruptedException {
        Ticket ticket = new Ticket();
        Event event = new Event();
        event.setEventType(EventType.OPERA);
        ticket.setEvent(event);
        ticket.setType(TicketType.BUDGETARY);

        when(validateTypes.compatibility(ticket)).thenReturn(false);

        var res = ticketService.addTicket(ticket).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
    }

    @Test
    void testFailAddEvent() throws ExecutionException, InterruptedException {
        Event event = new Event();
        event.setName("Bad event");
        event.setTicketsCount(1);

        when(eventRepository.save(any(Event.class))).thenThrow(new RuntimeException("failed"));

        var res = eventService.addEvent(event).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
    }

    @Test
    void testFailAddPersonOnLocationFailure() throws ExecutionException, InterruptedException {
        Person person = new Person();
        person.setPassportID("id");
        Location location = new Location();
        person.setLocation(location);

        when(locationRepository.save(any(Location.class))).thenThrow(new RuntimeException("fail"));
        when(personRepository.existsPersonByPassportID(any())).thenReturn(false);

        var res = personService.addPerson(person).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
    }

    @Test
    void testFailAddPersonOnPersonFailure() throws ExecutionException, InterruptedException {
        Person person = new Person();
        person.setPassportID("id");
        Location location = new Location();
        person.setLocation(location);

        when(locationRepository.save(any(Location.class))).thenReturn(location);
        when(personRepository.existsPersonByPassportID(any())).thenReturn(false);
        when(personRepository.save(any(Person.class))).thenThrow(new RuntimeException("fail"));

        var res = personService.addPerson(person).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
    }

    @Test
    void testFailAddPersonWithPassportId() throws ExecutionException, InterruptedException {
        Person person = new Person();
        person.setPassportID("duplicate");
        person.setLocation(new Location());

        when(personRepository.existsPersonByPassportID("duplicate")).thenReturn(true);

        var res = personService.addPerson(person).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
    }

    @Test
    void testFailAddVenue() throws ExecutionException, InterruptedException {
        Venue venue = new Venue();
        venue.setName("Fail Hall");

        when(venueRepository.save(any(Venue.class))).thenThrow(new RuntimeException("fail"));

        var res = venueService.addVenue(venue).get();

        assertFalse(Boolean.TRUE.equals(res.get("status")));
    }
}
