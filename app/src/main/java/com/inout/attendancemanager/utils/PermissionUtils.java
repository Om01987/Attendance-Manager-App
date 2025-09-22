package com.inout.attendancemanager.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;

public class PermissionUtils {

    // utils/PermissionUtils.java
    public static boolean hasAllRequiredPermissions(Context ctx) {
        // Always-required dangerous permissions
        String[] base = new String[] {
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE
        };
        for (String p : base) {
            if (ContextCompat.checkSelfPermission(ctx, p) != PackageManager.PERMISSION_GRANTED) return false;
        }

        // Storage logic:
        // If app ONLY saves its own photos (no gallery reads), no storage permission needed on API 29+.
        // If you DO read user images from shared storage, request granular media perms on API 33+.
        boolean needsReadFromGallery = false; // set true if you actually read existing media

        if (needsReadFromGallery) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // 33+
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED)
                    return false;
                // Add READ_MEDIA_VIDEO if needed
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // ≤28
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    return false;
            } else {
                // API 29–32: MediaStore read flows can work without legacy permission when using the Photo Picker.
                // If you insist on broad reads on 29–32, READ_EXTERNAL_STORAGE (maxSdkVersion 32) applies.
            }
        }

        // Android 12+ Bluetooth permissions (only if needed by your proximity feature)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                return false;
            if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
                return false;
        }

        return true;
    }

}
