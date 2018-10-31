package net.pool2go;

import java.io.Serializable;

/**
 * A wrapper for a location in the form of Latitude, Longitude and tied to an identifier.
 *
 * The identifier is intended to be in the form of {@code $DATE | IP}, to keep it as unique and simple as possible.
 */
public class LocationObject implements Serializable {

    private String key;
    private double latitude;
    private double longitude;

    /**
     * Create a LocationObject without an attached key.
     *
     * @param lat latitude of the location
     * @param lng longitude of the location
     */
    public LocationObject(double lat, double lng) {
        latitude = lat;
        longitude = lng;
        key = null;
    }

    /**
     * Create a LocationObject with an attached key.
     *
     * @param key the unique identifier
     * @param lat latitude of the location
     * @param lng longitude of the location
     */
    public LocationObject(String key, double lat, double lng) {
        this.key = key;
        latitude = lat;
        longitude = lng;
    }

    public String getKey() {
        return key;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setLatitude(double latitude) { this.latitude = latitude; }

    public void setLongitude(double longitude) { this.longitude = longitude; }
}
