package szp.rafael.tracking.stream.processors;

import org.apache.kafka.streams.processor.api.Record;
import org.apache.kafka.streams.processor.api.Processor;
import org.apache.kafka.streams.processor.api.ProcessorContext;
import szp.rafael.tracking.model.tracking.TrackingEvent;

public class ValidationProcessor implements Processor<String, TrackingEvent, String, TrackingEvent> {

    private ProcessorContext<String, TrackingEvent> context;

    @Override
    public void init(ProcessorContext<String, TrackingEvent> context) {
        this.context = context;
    }

    @Override
    public void process(Record<String, TrackingEvent> record) {
        // Normaliza key (placa) e valida eventTime
        if (record == null || record.value() == null) return;

        String placa = normalizePlaca(record.key() != null ? record.key() : record.value().getPlaca());
        long eventTime = record.value().getEventTime();

        if (placa == null || placa.isEmpty() || eventTime <= 0) {
            //TODO Log and metric
            return; // Descarta placas ou eventos inválidos
        }

        // forward with normalized key and same timestamp
        context.forward(record.withKey(placa));
    }

    @Override
    public void close() {}
    private String normalizePlaca(String raw) {
        if (raw == null) return null;
        return raw.trim().toUpperCase();
    }
}