package com.inout.attendancemanager.utils;

public class Constants {

    // ==================== SHARED PREFERENCES ====================

    // Main preferences file name
    public static final String PREF_NAME = "AttendanceManagerPrefs";

    // Core user data
    public static final String PREF_USER_ID = "user_id";
    public static final String PREF_USER_TYPE = "user_type";
    public static final String PREF_DEVICE_ID = "device_id";
    public static final String PREF_PHONE_NUMBER = "phone_number";

    // Employee profile data
    public static final String PREF_PROFILE_COMPLETED = "profile_completed";
    public static final String PREF_EMPLOYEE_ID = "employee_id";
    public static final String PREF_FULL_NAME = "full_name";
    public static final String PREF_DEPARTMENT = "department";
    public static final String PREF_DESIGNATION = "designation";
    public static final String PREF_PROFILE_IMAGE_URL = "profile_image_url";
    public static final String PREF_OFFICE_LOCATION = "office_location";
    public static final String PREF_REPORTING_MANAGER = "reporting_manager";

    // Phone verification
    public static final String PREF_PHONE_VERIFIED = "phone_verified";
    public static final String PREF_VERIFICATION_ID = "verification_id";
    public static final String PREF_VERIFICATION_TIME = "verification_time";

    // Permission status
    public static final String PREF_PERMISSIONS_GRANTED = "permissions_granted";
    public static final String PREF_PERMISSIONS_GRANTED_TIME = "permissions_granted_time";

    // ==================== DRAFT KEYS ====================

    // Draft employee registration data
    public static final String DRAFT_FULL_NAME = "draft_full_name";
    public static final String DRAFT_EMPLOYEE_ID = "draft_employee_id";
    public static final String DRAFT_DEPARTMENT = "draft_department";
    public static final String DRAFT_DESIGNATION = "draft_designation";
    public static final String DRAFT_JOIN_DATE = "draft_join_date";
    public static final String DRAFT_REPORTING_MANAGER = "draft_reporting_manager";
    public static final String DRAFT_OFFICE_LOCATION = "draft_office_location";
    public static final String DRAFT_EMERGENCY_CONTACT = "draft_emergency_contact";
    public static final String DRAFT_EMERGENCY_PHONE = "draft_emergency_phone";
    public static final String DRAFT_IMAGE_URI = "draft_image_uri";
    public static final String DRAFT_SAVED_TIME = "draft_saved_time";

    // ==================== FIREBASE COLLECTIONS ====================

    // Main collections
    public static final String COLLECTION_EMPLOYEES = "employees";
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_ATTENDANCE = "attendance";
    public static final String COLLECTION_LEAVES = "leaves";
    public static final String COLLECTION_DEVICES = "devices";
    public static final String COLLECTION_EMPLOYEE_APPROVALS = "employee_approvals";
    public static final String COLLECTION_ADMINS = "admins";
    public static final String COLLECTION_DEPARTMENTS = "departments";
    public static final String COLLECTION_OFFICE_LOCATIONS = "office_locations";

    // ==================== USER TYPES ====================

    public static final String USER_TYPE_ADMIN = "admin";
    public static final String USER_TYPE_EMPLOYEE = "employee";
    public static final String USER_TYPE_HR = "hr";
    public static final String USER_TYPE_MANAGER = "manager";

    // ==================== APPROVAL STATUS ====================

    public static final String STATUS_PENDING = "pending";
    public static final String STATUS_APPROVED = "approved";
    public static final String STATUS_REJECTED = "rejected";
    public static final String STATUS_UNDER_REVIEW = "under_review";

    // ==================== ATTENDANCE STATUS ====================

    public static final String ATTENDANCE_PRESENT = "present";
    public static final String ATTENDANCE_ABSENT = "absent";
    public static final String ATTENDANCE_LATE = "late";
    public static final String ATTENDANCE_HALF_DAY = "half_day";
    public static final String ATTENDANCE_ON_LEAVE = "on_leave";

    // ==================== VALIDATION CONSTANTS ====================

    // Employee ID validation
    public static final int MIN_EMPLOYEE_ID_LENGTH = 5;
    public static final int MAX_EMPLOYEE_ID_LENGTH = 10;

    // Name validation
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 50;

    // Phone number validation
    public static final int MIN_PHONE_LENGTH = 10;
    public static final int MAX_PHONE_LENGTH = 15;

    // Password validation (if used)
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 20;

    // ==================== REQUEST CODES ====================

    public static final int REQUEST_LOCATION_PERMISSION = 1001;
    public static final int REQUEST_CAMERA_PERMISSION = 1002;
    public static final int REQUEST_PHONE_PERMISSION = 1003;
    public static final int REQUEST_STORAGE_PERMISSION = 1004;
    public static final int REQUEST_NOTIFICATION_PERMISSION = 1005;

    // Activity result codes
    public static final int REQUEST_CODE_CAMERA = 2001;
    public static final int REQUEST_CODE_GALLERY = 2002;
    public static final int REQUEST_CODE_SETTINGS = 2003;
    public static final int REQUEST_CODE_EMPLOYEE_REGISTRATION = 2004;

    // ==================== NOTIFICATION CONSTANTS ====================

    public static final String NOTIFICATION_CHANNEL_ID = "attendance_notifications";
    public static final String NOTIFICATION_CHANNEL_NAME = "Attendance Notifications";
    public static final String NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for attendance reminders and updates";

    // ==================== FILE PATHS ====================

    // Firebase Storage paths
    public static final String STORAGE_PATH_PROFILE_IMAGES = "profile_images";
    public static final String STORAGE_PATH_DOCUMENTS = "documents";
    public static final String STORAGE_PATH_ATTENDANCE_PHOTOS = "attendance_photos";

    // ==================== DATE/TIME FORMATS ====================

    public static final String DATE_FORMAT_DISPLAY = "dd/MM/yyyy";
    public static final String DATE_FORMAT_API = "yyyy-MM-dd";
    public static final String TIME_FORMAT_DISPLAY = "hh:mm a";
    public static final String TIME_FORMAT_API = "HH:mm:ss";
    public static final String DATETIME_FORMAT_DISPLAY = "dd/MM/yyyy hh:mm a";
    public static final String DATETIME_FORMAT_API = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";

    // ==================== GEOFENCING CONSTANTS ====================

    public static final float GEOFENCE_RADIUS_METERS = 100.0f;
    public static final long GEOFENCE_EXPIRATION_MILLISECONDS = 24 * 60 * 60 * 1000; // 24 hours
    public static final String GEOFENCE_REQUEST_ID = "office_geofence";

    // ==================== NETWORK/API CONSTANTS ====================

    public static final int NETWORK_TIMEOUT_SECONDS = 30;
    public static final int MAX_RETRY_ATTEMPTS = 3;
    public static final long RETRY_DELAY_MILLISECONDS = 2000; // 2 seconds

    // ==================== IMAGE CONSTANTS ====================

    public static final int MAX_IMAGE_SIZE_MB = 5;
    public static final int MAX_IMAGE_SIZE_BYTES = MAX_IMAGE_SIZE_MB * 1024 * 1024;
    public static final int PROFILE_IMAGE_SIZE_PX = 300; // for compression
    public static final int ATTENDANCE_IMAGE_SIZE_PX = 800; // for compression

    // ==================== BUSINESS LOGIC CONSTANTS ====================

    // Working hours
    public static final String OFFICE_START_TIME = "09:00";
    public static final String OFFICE_END_TIME = "18:00";
    public static final String LUNCH_START_TIME = "13:00";
    public static final String LUNCH_END_TIME = "14:00";

    // Grace periods (in minutes)
    public static final int LATE_ARRIVAL_GRACE_MINUTES = 15;
    public static final int EARLY_DEPARTURE_GRACE_MINUTES = 15;

    // ==================== ERROR MESSAGES ====================

    public static final String ERROR_NETWORK = "Network error. Please check your connection.";
    public static final String ERROR_INVALID_DATA = "Invalid data provided.";
    public static final String ERROR_PERMISSION_DENIED = "Permission denied. Please grant required permissions.";
    public static final String ERROR_AUTHENTICATION_FAILED = "Authentication failed. Please try again.";
    public static final String ERROR_FILE_UPLOAD_FAILED = "File upload failed. Please try again.";

    // ==================== SUCCESS MESSAGES ====================

    public static final String SUCCESS_PROFILE_SAVED = "Profile saved successfully.";
    public static final String SUCCESS_ATTENDANCE_MARKED = "Attendance marked successfully.";
    public static final String SUCCESS_LEAVE_APPLIED = "Leave application submitted successfully.";

    // ==================== PRIVATE CONSTRUCTOR ====================

    // Prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }
}
