public class LocationObject {

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
}