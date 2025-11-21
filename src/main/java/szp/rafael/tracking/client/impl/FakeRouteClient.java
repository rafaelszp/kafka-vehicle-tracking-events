package szp.rafael.tracking.client.impl;

import szp.rafael.tracking.client.api.RouteEnrichmentClient;
import szp.rafael.tracking.client.model.ApiResponse;
import szp.rafael.tracking.model.store.RouteCacheValue;
import szp.rafael.tracking.model.tracking.TrackingEvent;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

public class FakeRouteClient implements RouteEnrichmentClient {

    private ThreadLocalRandom random = ThreadLocalRandom.current();

    @Override
    public ApiResponse<RouteCacheValue> fetchRoute(TrackingEvent event, String traceId, Duration timeout) {
        RouteCacheValue v = new RouteCacheValue();

        long plus = random.nextLong(1000, 50000);

        v.setRouteId("route-fake");
        v.setRouteJson("{\"path\":\"F->T\"}");
        v.setUpdatedAt(System.currentTimeMillis()+plus);

        return new ApiResponse<>(true, v, null);
    }
}
