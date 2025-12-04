package systems.project.services;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import systems.project.exceptions.InvalidDataException;
import systems.project.models.Ticket;
import systems.project.models.api.ImportResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Service
public class TicketService {

    private final TicketCommandService commands;

    public TicketService(TicketCommandService commands) {
        this.commands = commands;
    }

    @Async
    public CompletableFuture<Map<String, List<Ticket>>> getTickets() {
        return completedFuture(commands.getTickets());
    }

    @Async
    public CompletableFuture<Map<String, Boolean>> addTicket(Ticket ticket) {
        return completedFuture(commands.addTicket(ticket));
    }

    @Async
    public CompletableFuture<Ticket> getTicket(Integer id) {
        return completedFuture(commands.getTicket(id));
    }

    @Async
    public CompletableFuture<Boolean> updateTicket(Integer id, Ticket ticket) {
        return completedFuture(commands.updateTicket(id, ticket));
    }

    @Async
    public CompletableFuture<Boolean> removeTicket(Integer id) {
        return completedFuture(commands.removeTicket(id));
    }

    @Async
    public CompletableFuture<Boolean> deleteAllByComment(String comment) {
        return completedFuture(commands.deleteAllByComment(comment));
    }

    @Async
    public CompletableFuture<Ticket> getWithMinEvent() {
        return completedFuture(commands.getWithMinEvent());
    }

    @Async
    public CompletableFuture<Map<String, Long>> countByCommentLess(String comment) {
        return completedFuture(commands.countByCommentLess(comment));
    }

    @Async
    public CompletableFuture<Boolean> sellTicket(Integer ticketId, Integer personId, float amount) {
        return completedFuture(commands.sellTicket(ticketId, personId, amount));
    }

    @Async
    public CompletableFuture<Ticket> cloneVip(Integer ticketId) {
        return completedFuture(commands.cloneVip(ticketId));
    }

    @Async
    public CompletableFuture<ImportResult> importTickets(List<Ticket> tickets) {
        try {
            return completedFuture(commands.importTickets(tickets));
        } catch (InvalidDataException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
