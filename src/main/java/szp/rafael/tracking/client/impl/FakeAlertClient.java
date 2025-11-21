package szp.rafael.tracking.client.impl;

import szp.rafael.tracking.client.api.AlertEnrichmentClient;
import szp.rafael.tracking.client.model.ApiResponse;
import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.tracking.TrackingEvent;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class FakeAlertClient implements AlertEnrichmentClient {

    Logger logger  = Logger.getLogger(FakeAlertClient.class.getName());

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
        simulateFailure(traceId);
        return new ApiResponse<>(true, a, null);
    }

    private void simulateFailure(String traceId) {
        if(traceId.contains("error_sim")) {
            logger.info("\n\n\nSimulated failure: \n\n\n");
            throw new RuntimeException("\n\n\nSimulated failure");
        }
    }
}
