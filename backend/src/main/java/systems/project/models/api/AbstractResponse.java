package systems.project.models.api;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AbstractResponse<T> {

    private String title;

    private String message;

    private String status;

    private T data;

}
