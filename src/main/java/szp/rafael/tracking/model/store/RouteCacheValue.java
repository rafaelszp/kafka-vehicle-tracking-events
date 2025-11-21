package szp.rafael.tracking.model.store;

import java.io.Serializable;

public class RouteCacheValue implements Serializable {
    private String routeId;
    private String routeJson; // ou campos ricos
    private long updatedAt;

    public static RouteCacheValue defaultEmptyWithError(String error) {
        RouteCacheValue v = new RouteCacheValue();
        v.routeId = null;
        v.routeJson = "{}";
        v.updatedAt = System.currentTimeMillis();
        return v;
    }

    public String getRouteId() {
        return routeId;
    }

    public void setRouteId(String routeId) {
        this.routeId = routeId;
    }

    public String getRouteJson() {
        return routeJson;
    }

    public void setRouteJson(String routeJson) {
        this.routeJson = routeJson;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }
}
