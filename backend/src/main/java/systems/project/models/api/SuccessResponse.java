package systems.project.models.api;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SuccessResponse extends AbstractResponse {

    SuccessResponse(String title, String message, String status, Object data) {
        super(title, message, status, data);
    }
}
