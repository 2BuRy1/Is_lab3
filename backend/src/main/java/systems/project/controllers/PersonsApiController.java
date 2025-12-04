package systems.project.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RestController;
import systems.project.controllers.api.PersonsApi;
import systems.project.models.Person;
import systems.project.models.api.AbstractResponse;
import systems.project.models.envelopes.PersonEnvelope;
import systems.project.services.PersonService;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@CrossOrigin(origins = {"*"})
public class PersonsApiController implements PersonsApi {

    private final PersonService personService;

    public PersonsApiController(PersonService personService) {
        this.personService = personService;
    }

    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<Person>>> addPerson(Person person) {
        return personService.addPerson(person)
                .thenApply(res -> {
                    boolean ok = res != null && Boolean.TRUE.equals(res.get("status"));
                    if (ok) {
                        return ResponseEntity.ok(
                                AbstractResponse.<Person>builder()
                                        .status("ok")
                                        .title("Успех")
                                        .message("Человек создан")
                                        .data(person)
                                        .build()
                        );
                    }

                    boolean passportConflict = res != null && Boolean.TRUE.equals(res.get("passportId"));
                    if (passportConflict) {
                        return ResponseEntity.badRequest().body(
                                AbstractResponse.<Person>builder()
                                        .status("error")
                                        .title("Ошибка")
                                        .message("passportId должен быть уникален")
                                        .data(null)
                                        .build()
                        );
                    }

                    return ResponseEntity.badRequest().body(
                            AbstractResponse.<Person>builder()
                                    .status("error")
                                    .title("Ошибка")
                                    .message("Ошибка во время добавления")
                                    .data(null)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<Person>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }


    @Override
    public CompletableFuture<ResponseEntity<AbstractResponse<PersonEnvelope>>> getPersons() {
        return personService.getPersons()
                .thenApply(map -> {
                    List<Person> persons = map == null ? null : map.get("persons");
                    PersonEnvelope envelope = new PersonEnvelope();
                    envelope.setPersonList(persons);

                    return ResponseEntity.ok(
                            AbstractResponse.<PersonEnvelope>builder()
                                    .status("ok")
                                    .title("Успех")
                                    .message("Список людей")
                                    .data(envelope)
                                    .build()
                    );
                })
                .exceptionally(ex -> ResponseEntity.badRequest().body(
                        AbstractResponse.<PersonEnvelope>builder()
                                .status("error")
                                .title("Ошибка")
                                .message(ex.getMessage())
                                .data(null)
                                .build()
                ));
    }
}
