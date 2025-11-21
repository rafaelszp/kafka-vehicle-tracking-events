package szp.rafael.tracking.model.tracking;

import szp.rafael.tracking.model.AbstractModel;

import java.io.Serializable;

public class Geolocation  extends AbstractModel  implements Serializable{

    private double latitude;
    private double longitude;
    private double altitude;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(double altitude) {
        this.altitude = altitude;
    }
}
