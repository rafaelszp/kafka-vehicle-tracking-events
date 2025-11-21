package szp.rafael.tracking.client.impl;

import szp.rafael.tracking.client.api.AlertEnrichmentClient;
import szp.rafael.tracking.client.model.ApiResponse;
import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.tracking.TrackingEvent;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class FakeAlertClient implements AlertEnrichmentClient {


    public enum Level {
        LOW, MEDIUM, HIGH
    }

    ThreadLocalRandom random = ThreadLocalRandom.current();

    @Override
    public ApiResponse<AlertCacheValue> fetchAlert(TrackingEvent event, String traceId, Duration timeout) {
        AlertCacheValue a = new AlertCacheValue();
        Level level = Level.values()[random.nextInt(0, Level.values().length)];
        a.setLevel(level.name());
        a.setErrorFlag(false);
        a.setDetails("Alert of level "+level.name());
        a.updatedAt = System.currentTimeMillis();
        return new ApiResponse<>(true, a, null);
    }
}
