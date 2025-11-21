package szp.rafael.tracking.client.impl;

import szp.rafael.tracking.client.api.AlertEnrichmentClient;
import szp.rafael.tracking.client.model.ApiResponse;
import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.tracking.TrackingEvent;

import java.time.Duration;

public class FakeAlertClient implements AlertEnrichmentClient {
    @Override
    public ApiResponse<AlertCacheValue> fetchAlert(TrackingEvent event, String traceId, Duration timeout) {
        AlertCacheValue a = new AlertCacheValue();
        a.setLevel("MEDIUM");
        a.setErrorFlag(false);
        a.setDetails("ok");
        a.updatedAt = System.currentTimeMillis();
        return new ApiResponse<>(true, a, null);
    }
}
