package szp.rafael.tracking.model.store;

import java.io.Serializable;

public class PendingEntry implements Serializable {
    public enum Stage { ROUTE_ENRICH, ALERT_ENRICH }

    private byte[] originalEvent;
    private Stage stage;
    private int attempts;
    private long nextRetryAtMillis;
    private String traceId;
    private String lastError;
    private long createdAtMillis;

    // getters / setters
    public byte[] getOriginalEvent() { return originalEvent; }
    public void setOriginalEvent(byte[] originalEvent) { this.originalEvent = originalEvent; }
    public Stage getStage() { return stage; }
    public void setStage(Stage stage) { this.stage = stage; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public long getNextRetryAtMillis() { return nextRetryAtMillis; }
    public void setNextRetryAtMillis(long nextRetryAtMillis) { this.nextRetryAtMillis = nextRetryAtMillis; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public long getCreatedAtMillis() { return createdAtMillis; }
    public void setCreatedAtMillis(long createdAtMillis) { this.createdAtMillis = createdAtMillis; }
}
