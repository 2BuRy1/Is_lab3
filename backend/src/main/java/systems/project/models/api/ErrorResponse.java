package systems.project.models.api;


public class ErrorResponse extends AbstractResponse {

    ErrorResponse(String title, String message, String status, Object data) {
        super(title, message, status, data);
    }
}
