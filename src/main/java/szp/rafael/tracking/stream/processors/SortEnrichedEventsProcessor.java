package szp.rafael.tracking.stream.processors;

import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.state.KeyValueStore;
import org.jboss.logging.Logger;
import szp.rafael.tracking.helpers.CompositeKeyUtils;

import org.apache.kafka.streams.processor.PunctuationType;
import szp.rafael.tracking.model.tracking.EnrichedTrackingEvent;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;


public class SortEnrichedEventsProcessor implements Processor<String, EnrichedTrackingEvent, String, EnrichedTrackingEvent> {

    private static final Logger logger = Logger.getLogger(SortEnrichedEventsProcessor.class.getName());

    private ProcessorContext<String, EnrichedTrackingEvent> context;

    private final String watermarkStoreName ;
    private final String bufferStoreName;

    private KeyValueStore<Bytes, EnrichedTrackingEvent> bufferStore;
    private KeyValueStore<String, Long> watermarkStore;

    private final Duration windowSize = Duration.ofMinutes(5);

    public SortEnrichedEventsProcessor(String bufferStoreName, String watermarkStoreName) {
        this.bufferStoreName = bufferStoreName;
        this.watermarkStoreName = watermarkStoreName;
    }

    @Override
    public void init(ProcessorContext<String, EnrichedTrackingEvent> context) {
        this.context = context;
        this.bufferStore = context.getStateStore(bufferStoreName);
        this.watermarkStore = context.getStateStore(watermarkStoreName);

//         1. Agendador STREAM_TIME de baixa latência (Insight 3)
        this.context.schedule(
                Duration.ofSeconds(1),
                PunctuationType.STREAM_TIME,
                this::punctuateByStreamTime
        );

        // 2. Agendador WALL_CLOCK_TIME de "coleta de lixo" (Insight 3)
        this.context.schedule(
                Duration.ofMinutes(5),
                PunctuationType.WALL_CLOCK_TIME,
                this::punctuateByWallClock
        );

    }

    @Override
    public void process(Record<String, EnrichedTrackingEvent> record) {
        long ts = record.value().getOriginal().getEventTime();
        byte[] ck = CompositeKeyUtils.buildCompositeKey(record.key(), ts);

        bufferStore.put(Bytes.wrap(ck), record.value());
//        watermarkStore.put(record.key(), ts);
        watermarkStore.put(record.key(), record.timestamp()); //gravando hora que chegou

    }


    private void punctuateByWallClock(long wallClockTimestamp) {
        punctuateByStreamTime(wallClockTimestamp);
    }

    private void punctuateByStreamTime(long streamTime) {

        long effectiveTime = Math.max(context.currentStreamTimeMs(),streamTime);
        long windowCutoff = effectiveTime - windowSize.toMillis();

        try (var iter = watermarkStore.all()) {
            while (iter.hasNext()) {
                var kv = iter.next();
                String key = kv.key;
                long lastTs = kv.value;
//
                if (lastTs > windowCutoff) {
                    continue;
                }

                logger.info("EMIT WINDOW FOR key=" + key + " ts: "+ Instant.ofEpochMilli(lastTs) +" cutoff=" + Instant.ofEpochMilli(windowCutoff) + " effectiveStreamTime=" + Instant.ofEpochMilli(effectiveTime));
                emitWindowForKey(key, windowCutoff+1);
            }
        }

    }

    private void emitWindowForKey(String key, long cutoff) {

        byte[] from = CompositeKeyUtils.lowerBoundForKey(key);
        byte[] to   = CompositeKeyUtils.upperBoundForKey(key, cutoff);

        logger.info("\n\nrangeFrom="+ Arrays.toString(from) + "\nrangeTo..=" + Arrays.toString(to)+"\n\n");

        try (var it = bufferStore.range(Bytes.wrap(from), Bytes.wrap(to))) {

            int count = 0;
            while (it.hasNext()) {
                var kv = it.next();

                long originalTs = CompositeKeyUtils.extractTimestamp(kv.key.get());

                logger.debug("  ITER -> originalTs=" + originalTs + " keyBytes=" + Arrays.toString(kv.key.get())+" processedTime: "+ Instant.ofEpochMilli(kv.value.getProcessedAtMillis()));
                logger.debug("FORWARD -> originalTs=" + originalTs + " key=" + key);

                context.forward(new Record<>(key, kv.value, originalTs));
                bufferStore.delete(kv.key);
                count++;
            }
            logger.infof("bufferStore.range trouxe %s registros", count);
        }

        watermarkStore.delete(key);
    }


}

