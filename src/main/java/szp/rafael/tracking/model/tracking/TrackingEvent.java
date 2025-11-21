package szp.rafael.tracking.model.tracking;

import java.io.Serializable;

public class TrackingEvent implements Serializable {
    private String placa;
    private long eventTime;
    private String traceId;
    private Vehicle vehicle;
    // campos de geolocalização, clientId, vehicle object etc.


    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public long getEventTime() {
        return eventTime;
    }

    public void setEventTime(long eventTime) {
        this.eventTime = eventTime;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

}