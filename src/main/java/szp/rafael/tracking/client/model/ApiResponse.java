package szp.rafael.tracking.client.model;

public class ApiResponse<T> {
    final boolean success;
    final T value;
    final String error;

    public ApiResponse(boolean success, T value, String error) {
        this.success = success;
        this.value = value;
        this.error = error;
    }

    public boolean isSuccess() {
        return success;
    }

    public T getValue() {
        return value;
    }

    public String getError() {
        return error;
    }
}