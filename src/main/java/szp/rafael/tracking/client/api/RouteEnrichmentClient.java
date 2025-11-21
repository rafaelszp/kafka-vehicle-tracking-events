package szp.rafael.tracking.client.api;

import szp.rafael.tracking.client.model.ApiResponse;
import szp.rafael.tracking.model.store.RouteCacheValue;
import szp.rafael.tracking.model.tracking.TrackingEvent;

import java.time.Duration;

public interface RouteEnrichmentClient {
    ApiResponse<RouteCacheValue> fetchRoute(TrackingEvent event, String traceId, Duration timeout);
}
