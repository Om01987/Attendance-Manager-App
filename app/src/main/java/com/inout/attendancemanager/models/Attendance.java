package com.inout.attendancemanager.models;

import java.util.Date;

public class Attendance {
    private String userId;
    private String dateId; // yyyy-MM-dd format
    private Date inTime;
    private Date outTime;
    private long totalMinutes;
    private String status; // "present", "absent", "weekoff", "missed"
    private String deviceId;
    private String method; // "manual", "geofence", "qr"
    private double latitude;
    private double longitude;
    private Date createdAt;
    private Date updatedAt;

    // Empty constructor for Firestore
    public Attendance() {}

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDateId() { return dateId; }
    public void setDateId(String dateId) { this.dateId = dateId; }

    public Date getInTime() { return inTime; }
    public void setInTime(Date inTime) { this.inTime = inTime; }

    public Date getOutTime() { return outTime; }
    public void setOutTime(Date outTime) { this.outTime = outTime; }

    public long getTotalMinutes() { return totalMinutes; }
    public void setTotalMinutes(long totalMinutes) { this.totalMinutes = totalMinutes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    public Date getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Date updatedAt) { this.updatedAt = updatedAt; }
}
