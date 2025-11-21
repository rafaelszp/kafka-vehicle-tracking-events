package szp.rafael.tracking.stream;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.StoreBuilder;
import org.apache.kafka.streams.state.Stores;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jetbrains.annotations.NotNull;
import szp.rafael.tracking.helpers.AppKafkaConfig;
import szp.rafael.tracking.model.store.AlertCacheValue;
import szp.rafael.tracking.model.store.PendingEntry;
import szp.rafael.tracking.model.store.RouteCacheValue;
import szp.rafael.tracking.model.tracking.EnrichedTrackingEvent;
import szp.rafael.tracking.model.tracking.TrackingEvent;
import szp.rafael.tracking.stream.processors.EnrichmentCoordinatorProcessor;
import szp.rafael.tracking.stream.serializers.GsonSerde;

@ApplicationScoped
public class TopologyProducer {


    @Inject
    AppKafkaConfig config;

    @Inject
    ManagedExecutor executor;

    @Produces
    public Topology buildTopology() {
        GsonSerde<TrackingEvent> teSerde = TopologyProducer.getTrackingEventSerde();
        GsonSerde<RouteCacheValue> routeCacheValueGsonSerde = new GsonSerde<>(RouteCacheValue.class);
        GsonSerde<AlertCacheValue> alertCacheValueGsonSerde = new GsonSerde<>(AlertCacheValue.class);
        GsonSerde<PendingEntry> pendingRequestsCacheValueGsonSerde = new GsonSerde<>(PendingEntry.class);
        GsonSerde<EnrichedTrackingEvent> enrichedTrackingEventGsonSerde = new GsonSerde<>(EnrichedTrackingEvent.class);

        Topology topology = new Topology();

        String vehicleTrackingEvents = "Vehicle Tracking Events";
        topology.addSource(vehicleTrackingEvents, Serdes.String().deserializer(), teSerde.deserializer(), config.sourceTopic());

        String enrichmentProcessor = "Enrichment Processor";
        topology.addProcessor(enrichmentProcessor, () -> new EnrichmentCoordinatorProcessor(executor), vehicleTrackingEvents);

        topology.addStateStore(getRouteCacheStore(routeCacheValueGsonSerde),enrichmentProcessor);
        topology.addStateStore(getAlertCacheStore(alertCacheValueGsonSerde),enrichmentProcessor);
        topology.addStateStore(getPendingRequestsStore(pendingRequestsCacheValueGsonSerde),enrichmentProcessor);
        topology.addStateStore(getOrderBufferStore(enrichedTrackingEventGsonSerde),enrichmentProcessor);


        return topology;

    }

    private @NotNull StoreBuilder<KeyValueStore<String, RouteCacheValue>> getRouteCacheStore(GsonSerde<RouteCacheValue> routeCacheValueGsonSerde) {
        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(config.routeCacheStore()),
                Serdes.String(),
                routeCacheValueGsonSerde
        );
    }

    private @NotNull StoreBuilder<KeyValueStore<String, AlertCacheValue>> getAlertCacheStore(GsonSerde<AlertCacheValue> alertCacheValueGsonSerde) {
        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(config.alertCacheStore()),
                Serdes.String(),
                alertCacheValueGsonSerde
        );
    }

    private @NotNull StoreBuilder<KeyValueStore<String, PendingEntry>> getPendingRequestsStore(GsonSerde<PendingEntry> pendingRequestsCacheValueGsonSerde) {
        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(config.pendingRequestsStore()),
                Serdes.String(),
                pendingRequestsCacheValueGsonSerde
        );
    }

    private @NotNull StoreBuilder<KeyValueStore<String, EnrichedTrackingEvent>> getOrderBufferStore(GsonSerde<EnrichedTrackingEvent> enrichedTrackingEventGsonSerde) {
        return Stores.keyValueStoreBuilder(
                Stores.persistentKeyValueStore(config.orderBufferStore()),
                Serdes.String(),
                enrichedTrackingEventGsonSerde
        );
    }

    public static GsonSerde<TrackingEvent> getTrackingEventSerde() {
        return new GsonSerde<TrackingEvent>(TrackingEvent.class);
    }

}
