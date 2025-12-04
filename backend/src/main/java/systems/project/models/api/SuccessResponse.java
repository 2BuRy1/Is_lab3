package systems.project.models.api;

import lombok.Data;

@Data
public class SuccessResponse extends AbstractResponse {

    SuccessResponse(String title, String message, String status, Object data) {
        super(title, message, status, data);
    }
}
