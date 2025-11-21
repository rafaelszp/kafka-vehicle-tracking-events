package szp.rafael.tracking;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TestInputTopic;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.TopologyTestDriver;
import org.jboss.logging.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import szp.rafael.tracking.helpers.AppKafkaConfig;
import szp.rafael.tracking.model.tracking.TrackingEvent;
import szp.rafael.tracking.stream.TopologyProducer;
import szp.rafael.tracking.stream.serializers.GsonSerde;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

@QuarkusTest
public class MainAppTest {

    Properties streamProps = new Properties();

    List<TrackingEvent> events = new ArrayList<>();

    @Inject
    Logger logger;

    @Inject
    AppKafkaConfig config;

    @Inject
    Topology topology;


    @BeforeEach
    public void setup(){
        streamProps.put(StreamsConfig.APPLICATION_ID_CONFIG, "test-" + UUID.randomUUID());

        streamProps.put("auto.offset.reset", "earliest");
        streamProps.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamProps.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        streamProps.put(StreamsConfig.STATE_DIR_CONFIG, "/tmp/kafka-streams-"+this.getClass().getPackageName()+"-"+this.getClass().getSimpleName()+"/"+UUID.randomUUID());

        events = getEvents();

    }


    public List<TrackingEvent> getEvents() {
        List<TrackingEvent> events = this.events;

        for (int i = 0; i < 5; i++) {

            OffsetDateTime  dateTime = OffsetDateTime.of(2025, 10, 10, 0, 0, 0, 0, OffsetDateTime.now().getOffset());
            TrackingEvent event = new TrackingEvent();
            event.setTraceId(UUID.randomUUID().toString());
            event.setEventTime(dateTime.plusSeconds(i*10).toEpochSecond()*1000);
            event.setPlaca("AAA1111");
            events.add(event);
        }

        return events;
    }

    @Test
    public void testTopology(){

        logger.info(topology.describe());

        GsonSerde<TrackingEvent> teSerde = TopologyProducer.getTrackingEventSerde();

        try (final TopologyTestDriver testDriver = new TopologyTestDriver(topology, streamProps)) {

            TestInputTopic<String, TrackingEvent> inputTopic = testDriver.createInputTopic(config.sourceTopic(),
                    Serdes.String().serializer(), teSerde.serializer());

            for (TrackingEvent event : events) {
                inputTopic.pipeInput(event.getTraceId(), event);
            }


        }
    }

}
