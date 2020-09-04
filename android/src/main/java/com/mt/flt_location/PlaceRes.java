package com.mt.flt_location;

public class PlaceRes {
    private String name;
    private String thoroughfare;
    private String placeId;
    private double[] coordinate;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getThoroughfare() {
        return thoroughfare;
    }

    public void setThoroughfare(String thoroughfare) {
        this.thoroughfare = thoroughfare;
    }

    public double[] getCoordinate() {
        return coordinate;
    }

    public void setCoordinate(double[] coordinate) {
        this.coordinate = coordinate;
    }

    public String getPlaceId() {
        return placeId;
    }

    public void setPlaceId(String placeId) {
        this.placeId = placeId;
    }
}
