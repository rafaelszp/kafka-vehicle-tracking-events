package szp.rafael.tracking.stream.processors;

import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import szp.rafael.tracking.model.store.OrderBufferValue;
import szp.rafael.tracking.model.tracking.EnrichedTrackingEvent;
import szp.rafael.tracking.model.tracking.TrackingEvent;

import org.apache.kafka.streams.processor.PunctuationType;
import org.apache.kafka.streams.processor.api.*;
import org.apache.kafka.streams.state.KeyValueIterator;
import org.apache.kafka.streams.state.KeyValueStore;

import java.time.Duration;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class OutputFormatterProcessor implements Processor<String, EnrichedTrackingEvent, String, EnrichedTrackingEvent> {

    private static final Logger log = Logger.getLogger(OutputFormatterProcessor.class.getName());

    private ProcessorContext<String, EnrichedTrackingEvent> context;


    @Override
    public void init(ProcessorContext<String, EnrichedTrackingEvent> context) {
        this.context = context;
    }

    @Override
    public void process(Record<String, EnrichedTrackingEvent> record) {
        context.forward(record.withValue(record.value()));
    }
}

