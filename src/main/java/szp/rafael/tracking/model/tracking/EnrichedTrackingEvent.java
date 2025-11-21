package szp.rafael.tracking.model.tracking;

import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.store.RouteCacheValue;

import java.io.Serializable;

public class EnrichedTrackingEvent implements Serializable {
    private TrackingEvent original;
    private RouteCacheValue route;
    private AlertCacheValue alert;
    private int attemptsRoute;
    private int attemptsAlert;
    private String lastErrorRoute;
    private String lastErrorAlert;
    private long processedAtMillis;

    public static EnrichedTrackingEvent from(TrackingEvent original, RouteCacheValue route, AlertCacheValue alert) {
        EnrichedTrackingEvent et = new EnrichedTrackingEvent();
        et.original = original;
        et.route = route;
        et.alert = alert;
        et.attemptsRoute = 0;
        et.attemptsAlert = 0;
        et.lastErrorRoute = null;
        et.lastErrorAlert = null;
        et.processedAtMillis = System.currentTimeMillis();
        return et;
    }

    public TrackingEvent getOriginal() {
        return original;
    }

    public void setOriginal(TrackingEvent original) {
        this.original = original;
    }

    public RouteCacheValue getRoute() {
        return route;
    }

    public void setRoute(RouteCacheValue route) {
        this.route = route;
    }

    public AlertCacheValue getAlert() {
        return alert;
    }

    public void setAlert(AlertCacheValue alert) {
        this.alert = alert;
    }

    public int getAttemptsRoute() {
        return attemptsRoute;
    }

    public void setAttemptsRoute(int attemptsRoute) {
        this.attemptsRoute = attemptsRoute;
    }

    public int getAttemptsAlert() {
        return attemptsAlert;
    }

    public void setAttemptsAlert(int attemptsAlert) {
        this.attemptsAlert = attemptsAlert;
    }

    public String getLastErrorRoute() {
        return lastErrorRoute;
    }

    public void setLastErrorRoute(String lastErrorRoute) {
        this.lastErrorRoute = lastErrorRoute;
    }

    public String getLastErrorAlert() {
        return lastErrorAlert;
    }

    public void setLastErrorAlert(String lastErrorAlert) {
        this.lastErrorAlert = lastErrorAlert;
    }

    public long getProcessedAtMillis() {
        return processedAtMillis;
    }

    public void setProcessedAtMillis(long processedAtMillis) {
        this.processedAtMillis = processedAtMillis;
    }
}
