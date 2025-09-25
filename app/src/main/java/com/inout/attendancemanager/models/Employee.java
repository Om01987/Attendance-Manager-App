package com.inout.attendancemanager.models;

import java.util.Date;

/**
 * Employee profile model mapped from Firestore employees/{uid}.
 * - uid: Firestore document id (auth uid)
 * - userId: optional business user id (can mirror uid or differ)
 * - isAdmin: admin flag used by client routing and UI
 * - approvalStatus: "pending" | "approved" | "rejected"
 */
public class Employee {

    // Firestore document ID (Auth UID). Not stored by Firestore by default; set from code after fetch.
    private String uid;

    // Optional business identifier (can be same as uid or different)
    private String userId;

    // Profile fields
    private String employeeId;
    private String fullName;
    private String department;
    private String designation;
    private String joinDate;
    private String reportingManager;
    private String officeLocation;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String profileImageUrl;
    private String phoneNumber;
    private String deviceId;
    private Date registrationDate;

    // Status flags
    private boolean isActive;
    private String approvalStatus; // pending, approved, rejected

    // Admin flag for approval console access and admin routing
    private Boolean isAdmin;

    // Empty constructor for Firestore
    public Employee() {}

    // Getters and Setters

    // uid (doc id) - injected by client code after reading Firestore doc snapshot
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    // business user id
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public String getDesignation() { return designation; }
    public void setDesignation(String designation) { this.designation = designation; }

    public String getJoinDate() { return joinDate; }
    public void setJoinDate(String joinDate) { this.joinDate = joinDate; }

    public String getReportingManager() { return reportingManager; }
    public void setReportingManager(String reportingManager) { this.reportingManager = reportingManager; }

    public String getOfficeLocation() { return officeLocation; }
    public void setOfficeLocation(String officeLocation) { this.officeLocation = officeLocation; }

    public String getEmergencyContactName() { return emergencyContactName; }
    public void setEmergencyContactName(String emergencyContactName) { this.emergencyContactName = emergencyContactName; }

    public String getEmergencyContactPhone() { return emergencyContactPhone; }
    public void setEmergencyContactPhone(String emergencyContactPhone) { this.emergencyContactPhone = emergencyContactPhone; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public Date getRegistrationDate() { return registrationDate; }
    public void setRegistrationDate(Date registrationDate) { this.registrationDate = registrationDate; }

    public boolean isActive() { return isActive; }
    public void setIsActive(boolean active) { isActive = active; }

    public String getApprovalStatus() { return approvalStatus; }
    public void setApprovalStatus(String approvalStatus) { this.approvalStatus = approvalStatus; }

    // Admin flag. Use Boolean wrapper to allow null, then check with Boolean.TRUE.equals(...)
    public Boolean getIsAdmin() { return isAdmin; }
    public void setIsAdmin(Boolean isAdmin) { this.isAdmin = isAdmin; }
}
