package szp.rafael.tracking.client.api;

import szp.rafael.tracking.client.model.ApiResponse;
import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.tracking.TrackingEvent;

import java.time.Duration;

public interface AlertEnrichmentClient {
    ApiResponse<AlertCacheValue> fetchAlert(TrackingEvent event, String traceId, Duration timeout);

}
