package systems.project.services.core;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import systems.project.models.Venue;
import systems.project.services.command.VenueCommandService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Service
public class VenueService {

    private final VenueCommandService commands;

    public VenueService(VenueCommandService commands) {
        this.commands = commands;
    }

    @Async
    public CompletableFuture<Map<String, List<Venue>>> getVenues() {
        return completedFuture(commands.getVenues());
    }

    @Async
    public CompletableFuture<Map<String, Boolean>> addVenue(Venue venue) {
        return completedFuture(commands.addVenue(venue));
    }
}
