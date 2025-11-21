package szp.rafael.tracking.model.helper;

import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.store.PendingEntry;
import szp.rafael.tracking.model.store.RouteCacheValue;

public class  CompletedResult {
    final String pendingKey;
    final PendingEntry.Stage stage;
    final boolean success;
    final RouteCacheValue routeValue;
    final AlertCacheValue alertValue;
    final String errorMessage;

    private CompletedResult(String pendingKey, PendingEntry.Stage stage, boolean success,
                            RouteCacheValue routeValue, AlertCacheValue alertValue, String errorMessage) {
        this.pendingKey = pendingKey;
        this.stage = stage;
        this.success = success;
        this.routeValue = routeValue;
        this.alertValue = alertValue;
        this.errorMessage = errorMessage;
    }

    public static CompletedResult forRoute(String pendingKey, boolean success, RouteCacheValue routeValue, String error) {
        return new CompletedResult(pendingKey, PendingEntry.Stage.ROUTE_ENRICH, success, routeValue, null, error);
    }
    public static CompletedResult forAlert(String pendingKey, boolean success, AlertCacheValue alertValue, String error) {
        return new CompletedResult(pendingKey, PendingEntry.Stage.ALERT_ENRICH, success, null, alertValue, error);
    }
    public static CompletedResult forFailure(String pendingKey, PendingEntry.Stage stage, String error) {
        return new CompletedResult(pendingKey, stage, false, null, null, error);
    }

    public String getPendingKey() {
        return pendingKey;
    }

    public PendingEntry.Stage getStage() {
        return stage;
    }

    public boolean isSuccess() {
        return success;
    }

    public RouteCacheValue getRouteValue() {
        return routeValue;
    }

    public AlertCacheValue getAlertValue() {
        return alertValue;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
