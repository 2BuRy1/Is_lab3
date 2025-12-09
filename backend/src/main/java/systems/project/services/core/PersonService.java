package systems.project.services.core;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import systems.project.models.Person;
import systems.project.services.command.PersonCommandService;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

@Service
public class PersonService {

    private final PersonCommandService commands;

    public PersonService(PersonCommandService commands) {
        this.commands = commands;
    }

    @Async
    public CompletableFuture<Map<String, List<Person>>> getPersons() {
        return completedFuture(commands.getPersons());
    }

    @Async
    public CompletableFuture<Map<String, Boolean>> addPerson(Person person) {
        return completedFuture(commands.addPerson(person));
    }
}
