package systems.project.models.envelopes;

import lombok.Data;
import systems.project.models.Venue;

import java.util.List;

@Data
public class VenuesEnvelope {

    private List<Venue> venueList;

}
