package systems.project;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import systems.project.models.Coordinates;
import systems.project.models.Person;
import systems.project.models.Ticket;
import systems.project.models.TicketType;
import systems.project.repositories.EventRepository;
import systems.project.repositories.LocationRepository;
import systems.project.repositories.PersonRepository;
import systems.project.repositories.TicketRepository;
import systems.project.repositories.VenueRepository;
import systems.project.services.command.TicketCommandService;
import systems.project.services.core.TicketService;
import systems.project.services.core.ValidateTypes;
import systems.project.services.imports.TicketImportCoordinator;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketServiceTests {

    @Mock
    TicketRepository ticketRepository;

    @Mock
    PersonRepository personRepository;

    @Mock
    EventRepository eventRepository;

    @Mock
    ValidateTypes validateTypes;

    @Mock
    VenueRepository venueRepository;

    @Mock
    LocationRepository locationRepository;

    private TicketCommandService ticketCommandService;
    private TicketImportCoordinator ticketImportCoordinator;

    private TicketService ticketService;
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
        ticketImportCoordinator = org.mockito.Mockito.mock(TicketImportCoordinator.class);
        ticketService = new TicketService(ticketCommandService, ticketImportCoordinator);
    }

    @Test
    void testGetTicket() throws ExecutionException, InterruptedException {
        Ticket ticket = new Ticket();
        when(ticketRepository.findById(5)).thenReturn(Optional.of(ticket));

        var result = ticketService.getTicket(5).get();

        assertEquals(ticket, result);
    }

    @Test
    void testGetTicketEmpty() throws ExecutionException, InterruptedException {
        when(ticketRepository.findById(5)).thenReturn(Optional.empty());

        var result = ticketService.getTicket(5).get();

        assertNull(result);
    }

    @Test
    void testGetTicketException() throws ExecutionException, InterruptedException {
        when(ticketRepository.findById(5)).thenThrow(new RuntimeException("fail"));

        var result = ticketService.getTicket(5).get();

        assertNull(result);
    }

    @Test
    void testUpdateTicketSuccess() throws ExecutionException, InterruptedException {
        Ticket ticket = new Ticket();
        when(ticketRepository.existsById(5)).thenReturn(true);
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        var result = ticketService.updateTicket(5, ticket).get();

        assertTrue(result);
        verify(ticketRepository).save(ticket);
        assertEquals(5, ticket.getId());
    }

    @Test
    void testUpdateTicketNotFound() throws ExecutionException, InterruptedException {
        when(ticketRepository.existsById(5)).thenReturn(false);

        var result = ticketService.updateTicket(5, new Ticket()).get();

        assertFalse(result);
    }

    @Test
    void testUpdateTicketException() throws ExecutionException, InterruptedException {
        when(ticketRepository.existsById(5)).thenThrow(new RuntimeException("fail"));

        var result = ticketService.updateTicket(5, new Ticket()).get();

        assertFalse(result);
    }

    @Test
    void testRemoveTicketSuccess() throws ExecutionException, InterruptedException {
        when(ticketRepository.existsById(7)).thenReturn(true);

        var result = ticketService.removeTicket(7).get();

        assertTrue(result);
        verify(ticketRepository).deleteById(7);
    }

    @Test
    void testRemoveTicketNotFound() throws ExecutionException, InterruptedException {
        when(ticketRepository.existsById(7)).thenReturn(false);

        var result = ticketService.removeTicket(7).get();

        assertFalse(result);
    }

    @Test
    void testRemoveTicketException() throws ExecutionException, InterruptedException {
        when(ticketRepository.existsById(7)).thenReturn(true);
        doThrow(new RuntimeException()).when(ticketRepository).deleteById(7);

        var result = ticketService.removeTicket(7).get();

        assertFalse(result);
    }

    @Test
    void testDeleteAllByComment() throws ExecutionException, InterruptedException {
        when(ticketRepository.deleteByComment("match")).thenReturn(3L);

        var result = ticketService.deleteAllByComment("match").get();

        assertTrue(result);
        verify(ticketRepository).deleteByComment("match");
    }

    @Test
    void testDeleteAllByCommentNone() throws ExecutionException, InterruptedException {
        when(ticketRepository.deleteByComment("match")).thenReturn(0L);

        var result = ticketService.deleteAllByComment("match").get();

        assertFalse(result);
    }

    @Test
    void testDeleteAllByCommentEmpty() throws ExecutionException, InterruptedException {
        var result = ticketService.deleteAllByComment("   ").get();

        assertFalse(result);
    }

    @Test
    void testGetWithMinEvent() throws ExecutionException, InterruptedException {
        Ticket ticket = new Ticket();
        when(ticketRepository.findFirstByEventIsNotNullOrderByEventIdAsc())
                .thenReturn(Optional.of(ticket));

        var result = ticketService.getWithMinEvent().get();

        assertEquals(ticket, result);
    }

    @Test
    void testGetWithMinEventEmpty() throws ExecutionException, InterruptedException {
        when(ticketRepository.findFirstByEventIsNotNullOrderByEventIdAsc())
                .thenReturn(Optional.empty());

        var result = ticketService.getWithMinEvent().get();

        assertNull(result);
    }

    @Test
    void testCountByCommentLess() throws ExecutionException, InterruptedException {
        when(ticketRepository.countByCommentLessThan("abc")).thenReturn(7L);

        var result = ticketService.countByCommentLess("abc").get();

        assertEquals(7L, result.get("count"));
    }

    @Test
    void testCountByCommentLessException() throws ExecutionException, InterruptedException {
        when(ticketRepository.countByCommentLessThan("abc")).thenThrow(new RuntimeException());

        var result = ticketService.countByCommentLess("abc").get();

        assertEquals(0L, result.get("count"));
    }

    @Test
    void testSellTicketSuccess() throws ExecutionException, InterruptedException {
        Ticket ticket = new Ticket();
        Person person = new Person();

        when(ticketRepository.findById(1)).thenReturn(Optional.of(ticket));
        when(personRepository.findById(2L)).thenReturn(Optional.of(person));
        when(ticketRepository.save(any(Ticket.class))).thenReturn(ticket);

        var result = ticketService.sellTicket(1, 2, 150f).get();

        assertTrue(result);
        assertEquals(150f, ticket.getPrice());
        assertEquals(person, ticket.getPerson());
    }

    @Test
    void testSellTicketInvalidAmount() throws ExecutionException, InterruptedException {
        var result = ticketService.sellTicket(1, 2, -10f).get();

        assertFalse(result);
    }

    @Test
    void testSellTicketNoTicket() throws ExecutionException, InterruptedException {
        when(ticketRepository.findById(1)).thenReturn(Optional.empty());

        var result = ticketService.sellTicket(1, 2, 150f).get();

        assertFalse(result);
    }

    @Test
    void testSellTicketNoPerson() throws ExecutionException, InterruptedException {
        Ticket ticket = new Ticket();
        when(ticketRepository.findById(1)).thenReturn(Optional.of(ticket));
        when(personRepository.findById(2L)).thenReturn(Optional.empty());

        var result = ticketService.sellTicket(1, 2, 150f).get();

        assertFalse(result);
    }

    @Test
    void testCloneVipSuccess() throws ExecutionException, InterruptedException {
        Ticket original = new Ticket();
        original.setId(15);
        original.setName("Original");
        original.setPrice(20f);
        original.setType(TicketType.USUAL);
        Coordinates coords = new Coordinates();
        coords.setX(1);
        coords.setY(2f);
        original.setCoordinates(coords);

        when(ticketRepository.findById(15)).thenReturn(Optional.of(original));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var copy = ticketService.cloneVip(15).get();

        assertNotNull(copy);
        assertNull(copy.getId());
        assertEquals("Original", copy.getName());
        assertEquals(40f, copy.getPrice());
        assertEquals(TicketType.VIP, copy.getType());
        assertNotNull(copy.getCoordinates());
        assertNotSame(coords, copy.getCoordinates());
    }

    @Test
    void testCloneVipNotFound() throws ExecutionException, InterruptedException {
        when(ticketRepository.findById(10)).thenReturn(Optional.empty());

        var copy = ticketService.cloneVip(10).get();

        assertNull(copy);
    }
}
