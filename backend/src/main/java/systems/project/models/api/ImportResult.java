package systems.project.models.api;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private int requested;
    private int imported;
    private List<Integer> ticketIds;
    private Long logId;
    private String storageKey;
    private String filename;
}
