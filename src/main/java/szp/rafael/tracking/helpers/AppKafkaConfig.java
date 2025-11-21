package szp.rafael.tracking.helpers;

import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "app.config.kafka")
public interface AppKafkaConfig {

    @WithName("source_topic")
    String sourceTopic();

    @WithName("route_cache_store")
    String routeCacheStore();

    @WithName("alert_cache_store")
    String alertCacheStore();

    @WithName("pending_requests_store")
    String pendingRequestsStore();

    @WithName("order_buffer_store")
    String orderBufferStore();

    @WithName("sink_topic")
    String sinkTopic();
}
