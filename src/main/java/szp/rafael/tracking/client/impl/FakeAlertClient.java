package szp.rafael.tracking.client.impl;

import com.github.f4b6a3.ulid.UlidCreator;
import szp.rafael.tracking.client.api.AlertEnrichmentClient;
import szp.rafael.tracking.client.model.ApiResponse;
import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.tracking.TrackingEvent;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

public class FakeAlertClient implements AlertEnrichmentClient {

    Logger logger  = Logger.getLogger(FakeAlertClient.class.getName());

    public static final String SIM_ALERT_VALUE= "00000";
    public static ConcurrentHashMap<String,Integer> errorMap = new ConcurrentHashMap<>();

    public enum Level {
        LOW, MEDIUM, HIGH
    }

    static {
        errorMap.put(SIM_ALERT_VALUE, 0);
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
        if(traceId.contains(SIM_ALERT_VALUE) && errorMap.get(SIM_ALERT_VALUE)<1) {
            logger.info("Simulated failure: "+SIM_ALERT_VALUE);
            errorMap.put(SIM_ALERT_VALUE, errorMap.get(SIM_ALERT_VALUE) + 1);
            throw new RuntimeException("Simulated failure "+SIM_ALERT_VALUE);
        }else if(traceId.contains(SIM_ALERT_VALUE)) {
            logger.info("Clearing failure: "+SIM_ALERT_VALUE);
            errorMap.put(SIM_ALERT_VALUE, 0);
        }

    }
}
