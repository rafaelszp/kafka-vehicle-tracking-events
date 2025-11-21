package szp.rafael.tracking.model.store;

import java.io.Serializable;

public class AlertCacheValue implements Serializable {
    private String level; // NONE, LOW, MEDIUM, CRITICAL
    private boolean errorFlag;
    private String details;

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public boolean isErrorFlag() {
        return errorFlag;
    }

    public void setErrorFlag(boolean errorFlag) {
        this.errorFlag = errorFlag;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public long updatedAt;

    public static AlertCacheValue defaultNoneWithError(String error) {
        AlertCacheValue a = new AlertCacheValue();
        a.level = "NONE";
        a.errorFlag = true;
        a.details = error;
        a.updatedAt = System.currentTimeMillis();
        return a;
    }
}
