package systems.project.models.envelopes;

import lombok.Data;
import systems.project.models.Event;

import java.util.List;

@Data
public class EventsEnvelope {

    private List<Event> eventList;
}
