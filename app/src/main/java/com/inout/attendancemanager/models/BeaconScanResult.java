package com.inout.attendancemanager.models;

public class BeaconScanResult {
    private final String beaconId;
    private final int rssi;
    private final String label;
    private final boolean isValid;

    public BeaconScanResult(String beaconId, int rssi, String label, boolean isValid) {
        this.beaconId = beaconId;
        this.rssi = rssi;
        this.label = label;
        this.isValid = isValid;
    }

    public String getBeaconId() { return beaconId; }
    public int getRssi() { return rssi; }
    public String getLabel() { return label; }
    public boolean isValid() { return isValid; }

    public static BeaconScanResult invalid() {
        return new BeaconScanResult(null, -999, null, false);
    }
}
