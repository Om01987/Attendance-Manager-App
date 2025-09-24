package com.inout.attendancemanager.utils;

import android.text.TextUtils;
import java.util.regex.Pattern;

public class ValidationUtils {

    private static final Pattern EMPLOYEE_ID_PATTERN =
            Pattern.compile("^[A-Z]{2,3}[0-9]{3,6}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[+]?[0-9]{10,13}$");

    public static boolean isValidEmployeeId(String employeeId) {
        if (TextUtils.isEmpty(employeeId)) {
            return false;
        }
        return EMPLOYEE_ID_PATTERN.matcher(employeeId.toUpperCase()).matches();
    }

    public static boolean isValidPhoneNumber(String phoneNumber) {
        if (TextUtils.isEmpty(phoneNumber)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phoneNumber.replaceAll("\\s+", "")).matches();
    }

    public static boolean isValidName(String name) {
        return !TextUtils.isEmpty(name) && name.trim().length() >= 2;
    }
}