package com.inout.attendancemanager.utils;

import android.Manifest;

public class PermissionConstants {
    // Permission request codes
    public static final int PERMISSION_REQUEST_CODE = 1001;

    // Required permissions array
    public static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    // Permission descriptions for user understanding
    public static final String CAMERA_PERMISSION_DESC = "Camera access is required to capture your photo during attendance marking for security verification.";
    public static final String LOCATION_PERMISSION_DESC = "Location access is required to verify you are at the office premises when marking attendance.";
    public static final String PHONE_PERMISSION_DESC = "Phone access is required to generate a unique device identifier for security purposes.";
    public static final String STORAGE_PERMISSION_DESC = "Storage access is required to save your profile picture and attendance photos.";

    // Permission titles
    public static final String CAMERA_PERMISSION_TITLE = "Camera Permission";
    public static final String LOCATION_PERMISSION_TITLE = "Location Permission";
    public static final String PHONE_PERMISSION_TITLE = "Phone Permission";
    public static final String STORAGE_PERMISSION_TITLE = "Storage Permission";
}
