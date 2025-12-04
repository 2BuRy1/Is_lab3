package systems.project.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import systems.project.controllers.api.VenuesApi;
import systems.project.models.Venue;
import systems.project.models.api.AbstractResponse;
import systems.project.models.envelopes.VenuesEnvelope;
import systems.project.services.VenueService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = {"*"})
public class VenuesApiController implements VenuesApi {

    private final VenueService venueService;

    public VenuesApiController(VenueService venueService) {
        this.venueService = venueService;
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Venue>>> addVenue(Venue venue) {
        return venueService.addVenue(venue)
                .thenApply(res -> {
                    boolean ok = res != null && Boolean.TRUE.equals(res.get("status"));
                    if (ok) {
                        return ResponseEntity.ok(
                                AbstractResponse.<Venue>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Площадка создана")
                                        .data(venue)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Venue>builder()
                                    .status("error")
                                    .title("Ошибка")
                                    .message("Ошибка при создании площадки")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Venue>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<VenuesEnvelope>>> getVenues() {
        return venueService.getVenues()
                .thenApply(map -> {
                    List<Venue> venues = map == null ? null : map.get("venues");
                    VenuesEnvelope envelope = new VenuesEnvelope();
                    envelope.setVenueList(venues);

                    return ResponseEntity.ok(
                            AbstractResponse.<VenuesEnvelope>builder()
                                    .status("ok")
                                    .title("Успех")
                                    .message("Список площадок")
                                    .data(envelope)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<VenuesEnvelope>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }
}
