package szp.rafael.tracking.model.tracking;

import szp.rafael.tracking.model.AbstractModel;

import java.io.Serializable;

public class TrackingEvent  extends AbstractModel implements Serializable {

    public String eventId;
    private String placa;
    private long eventTime;
    private String traceId;
    private Vehicle vehicle;
    // campos de geolocalização, clientId, vehicle object etc.


    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public Vehicle getVehicle() {
        return vehicle;
    }

    public void setVehicle(Vehicle vehicle) {
        this.vehicle = vehicle;
    }

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