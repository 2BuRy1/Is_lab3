package systems.project.models.envelopes;

import lombok.Data;
import systems.project.models.Person;

import java.util.List;

@Data
public class PersonEnvelope {

    private List<Person> personList;
}
