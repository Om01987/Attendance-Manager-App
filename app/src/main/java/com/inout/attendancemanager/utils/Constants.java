package com.inout.attendancemanager.utils;

public class Constants {
    // Firebase Collections
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_ATTENDANCE = "attendance";
    public static final String COLLECTION_LEAVES = "leaves";
    public static final String COLLECTION_DEVICES = "devices";

    // Shared Preferences
    public static final String PREF_NAME = "AttendanceApp";
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_TYPE = "user_type";
    public static final String PREF_DEVICE_ID = "device_id";
    public static final String PREF_PHONE_NUMBER = "phone_number";

    // User Types
    public static final String USER_TYPE_ADMIN = "admin";
    public static final String USER_TYPE_EMPLOYEE = "employee";

    // Request Codes
    public static final int REQUEST_LOCATION_PERMISSION = 1001;
    public static final int REQUEST_CAMERA_PERMISSION = 1002;
    public static final int REQUEST_PHONE_PERMISSION = 1003;

}

