package com.inout.attendancemanager.models;

public class BeaconConfig {
    private String beaconId;
    private String uuid;
    private int major;
    private int minor;
    private int rssiThreshold; // e.g., -70
    private String label;
    private boolean enabled;

    public BeaconConfig() {}

    // Getters and setters
    public String getBeaconId() { return beaconId; }
    public void setBeaconId(String beaconId) { this.beaconId = beaconId; }

    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }

    public int getMajor() { return major; }
    public void setMajor(int major) { this.major = major; }

    public int getMinor() { return minor; }
    public void setMinor(int minor) { this.minor = minor; }

    public int getRssiThreshold() { return rssiThreshold; }
    public void setRssiThreshold(int rssiThreshold) { this.rssiThreshold = rssiThreshold; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
