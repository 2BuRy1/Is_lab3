package systems.project.services.imports;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.springframework.stereotype.Component;
import systems.project.exceptions.InvalidDataException;
import systems.project.models.Ticket;

import java.io.IOException;
import java.util.List;

@Component
public class TicketImportParser {

    private static final TypeReference<List<Ticket>> TICKET_LIST = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    public TicketImportParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Ticket> parse(byte[] payload) throws InvalidDataException {
        try {
            JsonNode node = objectMapper.readTree(payload);
            ArrayNode arrayNode = extractArray(node);
            if (arrayNode == null || arrayNode.isEmpty()) {
                throw new InvalidDataException("JSON не содержит массив ticket");
            }
            byte[] normalized = objectMapper.writeValueAsBytes(arrayNode);
            return objectMapper.readValue(normalized, TICKET_LIST);
        } catch (IOException e) {
            throw new InvalidDataException("Файл не является корректным JSON", e);
        }
    }

    private ArrayNode extractArray(JsonNode node) {
        if (node == null) {
            return null;
        }
        if (node.isArray()) {
            return (ArrayNode) node;
        }
        if (node.hasNonNull("ticketList")) {
            return extractArray(node.get("ticketList"));
        }
        if (node.hasNonNull("tickets")) {
            return extractArray(node.get("tickets"));
        }
        if (node.hasNonNull("data")) {
            return extractArray(node.get("data"));
        }
        return null;
    }
}
