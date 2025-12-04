package systems.project.models.envelopes;

import lombok.Data;
import systems.project.models.Ticket;

import java.util.List;

@Data
public class TicketsEnvelope {

    private List<Ticket> ticketList;

}
