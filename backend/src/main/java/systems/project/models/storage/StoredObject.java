package systems.project.models.storage;

import io.minio.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;

public class StoredObject implements AutoCloseable {

    private final GetObjectResponse response;

    public StoredObject(GetObjectResponse response) {
        this.response = response;
    }

    public InputStream stream() {
        return response;
    }

    @Override
    public void close() throws IOException {
        response.close();
    }
}
