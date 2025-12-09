package systems.project.services.core;

import org.springframework.stereotype.Service;
import systems.project.models.Ticket;
import systems.project.models.Event;
import systems.project.models.TicketType;
import systems.project.models.EventType;
import systems.project.models.Venue;
import systems.project.models.VenueType;






@Service
public class ValidateTypes {


    public boolean compatibility(Ticket ticket) {
        return checkTicketAndEventCompatibility(ticket) && checkVenueAndEventCompatibility(ticket);
    }

    private boolean checkTicketAndEventCompatibility(Ticket ticket) {
        Event event = ticket.getEvent();
        TicketType ticketType = ticket.getType();

        if (event == null || event.getEventType() == null) return true;
        EventType eventType = event.getEventType();

        return switch (eventType) {
            case FOOTBALL -> ticketType.equals(TicketType.CHEAP)
                    || ticketType.equals(TicketType.USUAL);
            case BASEBALL -> ticketType.equals(TicketType.USUAL)
                    || ticketType.equals(TicketType.VIP)
                    || ticketType.equals(TicketType.CHEAP);
            case BASKETBALL, OPERA ->  ticketType.equals(TicketType.USUAL)
                    || ticketType.equals(TicketType.VIP);
            case CONCERT -> ticketType.equals(TicketType.USUAL)
                    || ticketType.equals(TicketType.VIP)
                    || ticketType.equals(TicketType.BUDGETARY);
            
        };
        
    }
    
    private boolean checkVenueAndEventCompatibility(Ticket ticket) {
        Event event = ticket.getEvent();
        Venue venue = ticket.getVenue();
        
        
        if (event == null || venue == null
                || event.getEventType() == null
                || venue.getType() == null) return true;
        
        EventType eventType = event.getEventType(); 
        VenueType venueType = venue.getType(); 
        
        return switch (eventType) {
            case FOOTBALL, BASEBALL, BASKETBALL, OPERA -> venueType.equals(VenueType.STADIUM);
            case CONCERT -> venueType.equals(VenueType.LOFT) || venueType.equals(VenueType.OPEN_AREA);
        };

        
        
    }


}
