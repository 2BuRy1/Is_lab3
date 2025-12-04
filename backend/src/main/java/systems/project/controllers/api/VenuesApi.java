package systems.project.controllers.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Generated;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.context.request.NativeWebRequest;
import systems.project.models.api.AbstractResponse;

import systems.project.models.Venue;
import systems.project.models.envelopes.VenuesEnvelope;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen",
        date = "2025-09-21T19:04:05.004649+03:00[Europe/Moscow]",
        comments = "Generator version: 7.6.0")
@Validated
@Tag(name = "Venues", description = "the Venues API")
public interface VenuesApi {

    default Optional<NativeWebRequest> getRequest() {
        return Optional.empty();
    }

    /**
     * POST /add_venue : Создать площадку
     *
     * @param venue (required)
     * @return Создано (status code 200)
     * or Ошибка (status code 400)
     */
    @Operation(
        operationId = "addVenue",
        summary = "Создать площадку",
        tags = { "Venues" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Создано", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = AbstractResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Ошибка", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = AbstractResponse.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.POST,
        value = "/add_venue",
        produces = { "application/json" },
        consumes = { "application/json" }
    )
    CompletableFuture<ResponseEntity<AbstractResponse<Venue>>> addVenue(
        @Parameter(name = "Venue", description = "", required = true) @Valid @RequestBody Venue venue
    );


    /**
     * GET /get_venues : Получить список площадок
     *
     * @return Успех (status code 200)
     *         or Ошибка (status code 400)
     */
    @Operation(
        operationId = "getVenues",
        summary = "Получить список площадок",
        tags = { "Venues" },
        responses = {
            @ApiResponse(responseCode = "200", description = "Успех", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = AbstractResponse.class))
            }),
            @ApiResponse(responseCode = "400", description = "Ошибка", content = {
                @Content(mediaType = "application/json", schema = @Schema(implementation = AbstractResponse.class))
            })
        }
    )
    @RequestMapping(
        method = RequestMethod.GET,
        value = "/get_venues",
        produces = { "application/json" }
    )

    CompletableFuture<ResponseEntity<AbstractResponse<VenuesEnvelope>>> getVenues();

}
