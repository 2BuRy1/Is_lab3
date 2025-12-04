package systems.project.controllers;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import systems.project.controllers.api.TicketsApi;
import systems.project.models.Ticket;
import systems.project.models.api.AbstractResponse;
import systems.project.models.api.CloneRequest;
import systems.project.models.api.ImportResult;
import systems.project.models.api.SellRequestDTO;
import systems.project.models.envelopes.TicketsEnvelope;
import systems.project.services.TicketEventService;
import systems.project.services.TicketService;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = {"*"})
@Slf4j
public class TicketsApiController implements TicketsApi {

    private final TicketService ticketService;
    private final TicketEventService events;

    public TicketsApiController(TicketService ticketService, TicketEventService events) {
        this.ticketService = ticketService;
        this.events = events;
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Ticket>>> addTicket(Ticket ticket) {
        return ticketService.addTicket(ticket)
                .thenApply(res -> {
                    boolean ok = res != null && Boolean.TRUE.equals(res.get("status"));
                    if (ok) {
                        events.publishChange("add", null);
                        return ResponseEntity.ok(
                                AbstractResponse.<Ticket>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Билет создан")
                                        .data(ticket)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Ticket>builder()
                                    .status("error")
                                    .title("Ошибка")
                                    .message("Ошибка при создании билета")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Ticket>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Ticket>>> cloneVip(CloneRequest cloneRequest) {
        return ticketService.cloneVip(cloneRequest.getTicketId())
                .thenApply(copy -> {
                    if (copy != null) {
                        events.publishChange("vip-clone", null);
                        return ResponseEntity.ok(
                                AbstractResponse.<Ticket>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("VIP-копия создана")
                                        .data(copy)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Ticket>builder()
                                    .status("error")
                                    .title("Не найдено")
                                    .message("Исходный билет не найден")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Ticket>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Integer>>> countCommentLess(String comment) {
        return ticketService.countByCommentLess(comment)
                .thenApply(map -> {
                    long cnt = map == null ? 0L : Optional.ofNullable(map.get("count")).orElse(0L);
                    return ResponseEntity.ok(
                            AbstractResponse.<Integer>builder()
                                    .status("ok")
                                    .title("Успех")
                                    .message("Подсчитано")
                                    .data((int) cnt)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.
                        badRequest().body(
                        AbstractResponse.<Integer>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Void>>> deleteByComment(String commentEq) {
        return ticketService.deleteAllByComment(commentEq)
                .thenApply(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        events.publishChange("bulk-delete", null);
                        return ResponseEntity.ok(
                                AbstractResponse.<Void>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Удаление по комментарию выполнено")
                                        .data(null)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Void>builder()
                                    .status("error")
                                    .title("Не найдено")
                                    .message("Не найдено билетов с таким comment")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Void>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Void>>> deleteTicket(Integer id) {
        return ticketService.removeTicket(id)
                .thenApply(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        events.publishChange("delete", id);
                        return ResponseEntity.ok(
                                AbstractResponse.<Void>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Билет удалён")
                                        .data(null)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Void>builder()
                                    .status("error")
                                    .title("Не найдено")
                                    .message("Ошибка при удалении объекта, возможно его не существует")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Void>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Ticket>>> getTicketById(Integer id) {
        return ticketService.getTicket(id)
                .thenApply(t -> {
                    if (t != null) {
                        return ResponseEntity.ok(
                                AbstractResponse.<Ticket>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Билет найден")
                                        .data(t)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Ticket>builder()
                                    .status("error")
                                    .title("Не найдено")
                                    .message("Билет не найден")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Ticket>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<TicketsEnvelope>>> getTickets() {
        return ticketService.getTickets()
                .thenApply(map -> {
                    List<Ticket> tickets = map == null ? null : map.get("tickets");
                    TicketsEnvelope env = new TicketsEnvelope();
                    env.setTicketList(tickets);

                    return ResponseEntity.ok(
                            AbstractResponse.<TicketsEnvelope>builder()
                                    .status("ok")
                                    .title("Успех")
                                    .message("Список билетов")
                                    .data(env)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<TicketsEnvelope>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity
            <AbstractResponse<ImportResult>>>
            importTickets(@Valid List<Ticket> ticketList) {
        return ticketService.importTickets(ticketList)
                .thenApply(result -> {
                    log.info(ticketList.toString());
                    events.publishChange("bulk-import", null);
                    return ResponseEntity.ok(
                            AbstractResponse.<ImportResult>builder()
                                    .status("ok")
                                    .title("Успех")
                                    .message("Импорт выполнен")
                                    .data(result)
                                    .build()
                    );
                })
                .exceptionally(ex -> {
                    log.info("error occured");
                    Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
                    String message = cause.getMessage() != null ? cause.getMessage() : "Ошибка импорта";
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<ImportResult>builder()
                                    .status("error")
                                    .title("Ошибка импорта")
                                    .message(message)
                                    .data(null)
                                    .build()
                    );
                });
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Ticket>>> minEventTicket() {
        return ticketService.getWithMinEvent()
                .thenApply(t -> {
                    if (t != null) {
                        return ResponseEntity.ok(
                                AbstractResponse.<Ticket>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Минимальный по событию билет")
                                        .data(t)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Ticket>builder()
                                    .status("error")
                                    .title("Не найдено")
                                    .message("Не найден билет с событием")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Ticket>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Void>>> sellTicket(SellRequestDTO req) {
        return ticketService.sellTicket(req.getTicketId(), req.getPersonId(), req.getAmount())
                .thenApply(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        events.publishChange("ticket-sell", null);
                        return ResponseEntity.ok(
                                AbstractResponse.<Void>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Билет продан")
                                        .data(null)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Void>builder()
                                    .status("error")
                                    .title("Ошибка")
                                    .message("Продажа не выполнена")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Void>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }

    @Override
    public SseEmitter stream() {
        return events.subscribe();
    }


    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Void>>> updateTicket(Integer id, Ticket ticket) {
        return ticketService.updateTicket(id, ticket)
                .thenApply(ok -> {
                    if (Boolean.TRUE.equals(ok)) {
                        events.publishChange("update", id);
                        return ResponseEntity.ok(
                                AbstractResponse.<Void>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Билет обновлён")
                                        .data(null)
                                        .build()
                        );
                    }
                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Void>builder()
                                    .status("error")
                                    .title("Не найдено")
                                    .message("Билет не найден или не обновлён")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Void>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }
}
