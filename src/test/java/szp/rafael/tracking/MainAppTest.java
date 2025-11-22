package szp.rafael.tracking;

import com.github.f4b6a3.ulid.UlidCreator;
import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.context.api.NamedInstance;
import jakarta.inject.Inject;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import szp.rafael.tracking.client.impl.FakeAlertClient;
import szp.rafael.tracking.helpers.AppKafkaConfig;
import szp.rafael.tracking.model.tracking.EnrichedTrackingEvent;
import szp.rafael.tracking.model.tracking.TrackingEvent;
import szp.rafael.tracking.stream.TopologyProducer;
import szp.rafael.tracking.stream.processors.EnrichmentCoordinatorProcessor;
import szp.rafael.tracking.stream.serializers.GsonSerde;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@QuarkusTest
public class MainAppTest {

    Properties streamProps = new Properties();

    List<TrackingEvent> events = new ArrayList<>();

    @Inject
    Logger logger;

    @Inject
    AppKafkaConfig config;

    @Inject
    @NamedInstance("topologyExecutor")
    ManagedExecutor executor;

    ThreadLocalRandom random = ThreadLocalRandom.current();


    @Inject
    Topology topology;

    GsonSerde<EnrichedTrackingEvent> enrichedTrackingEventGsonSerde = new GsonSerde<>(EnrichedTrackingEvent.class);
    GsonSerde<TrackingEvent> teSerde = TopologyProducer.getTrackingEventSerde();

    @BeforeEach
    public void setup(){
        streamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-" + UUID.randomUUID());

        streamProps.put("auto.offset.reset", "earliest");
        streamProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamProps.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams-"+this.getClass().getPackageName()+"-"+this.getClass().getSimpleName()+"/"+UlidCreator.getMonotonicUlid().toLowerCase());

        events = getOutOfOrderEvents();

    }


    public List<TrackingEvent> getOutOfOrderEvents() {
        List<TrackingEvent> events = this.events;

        for (int i = 0; i < 5; i++) {

            int delta = Math.min(random.nextInt(0,10*(i+1)),30);
            OffsetDateTime  dateTime = OffsetDateTime.of(1970, 1, 1, 1, 0, 0, 0, ZoneOffset.UTC);
            TrackingEvent event = new TrackingEvent();
            event.setEventId(UlidCreator.getMonotonicUlid().toLowerCase());
            event.setTraceId(UUID.randomUUID().toString());

            if(random.nextBoolean()) {
                event.setEventTime(dateTime.plusSeconds(delta).toEpochSecond() * 1000);
            }else{
                event.setEventTime(dateTime.minusSeconds(delta).toEpochSecond() * 1000);
            }
            if(i==4){
                event.setTraceId(FakeAlertClient.SIM_ALERT_VALUE);
                event.setEventTime(dateTime.toEpochSecond()*1000);
            }
            event.setPlaca("AAA1111");
            events.add(event);
        }

        return events;
    }

    @Test
    public void testTopology(){

        logger.info(topology.describe());

        try (final TopologyTestDriver testDriver = new TopologyTestDriver(topology, streamProps)) {

            TestInputTopic<String, TrackingEvent> inputTopic = testDriver.createInputTopic(config.sourceTopic(),
                    Serdes.String().serializer(), teSerde.serializer());

            for (TrackingEvent event : events) {
                inputTopic.pipeInput(event.getPlaca(), event);
            }

            var outputTopic = testDriver.createOutputTopic(config.sinkTopic(), Serdes.String().deserializer(), enrichedTrackingEventGsonSerde.deserializer());

            waitCompletion(testDriver);

            long advanceBy = Duration.ofMinutes(6).toMillis();
            testDriver.advanceWallClockTime(Duration.ofMillis(advanceBy));

            List<EnrichedTrackingEvent> enrichedList = outputTopic.readValuesToList();

            logger.info("Enriched events: "+enrichedList.size());
            enrichedList.forEach(e -> logger.info(e.toJSONString()));

        }
    }

    private void waitCompletion(TopologyTestDriver testDriver) {
        try {
            Duration advance = Duration.of(60_001, ChronoUnit.MILLIS);
            testDriver.advanceWallClockTime(advance);
            for(int i=0; i<events.size()+1; i++){
                logger.debugf("waiting event %s",i);
                boolean b = executor.awaitTermination(EnrichmentCoordinatorProcessor.calculateBackoff(config.backoffMs(),config.maxAttempts())+config.puntuateIntervalMs(), TimeUnit.MILLISECONDS);
                testDriver.advanceWallClockTime(advance);
                logger.debugf("Done waiting event %s: timed out: %s",i,(!b)+"");

            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

}
