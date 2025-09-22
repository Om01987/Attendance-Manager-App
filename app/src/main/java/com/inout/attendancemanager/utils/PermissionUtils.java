package com.inout.attendancemanager.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.content.ContextCompat;
import java.util.ArrayList;
import java.util.List;

public class PermissionUtils {

    // Set true only if reading user gallery files is required now; Photo Picker needs no permission.
    public static final boolean NEEDS_GALLERY_READ = false;

    // Request only core permissions; accept coarse OR fine for location.
    public static String[] buildRequiredPermissions(Context ctx) {
        List<String> req = new ArrayList<>();

        // Core for this phase
        req.add(Manifest.permission.CAMERA);
        // Request both; we'll accept either one being granted
        req.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        req.add(Manifest.permission.ACCESS_FINE_LOCATION);

        // READ_PHONE_STATE is not needed to get ANDROID_ID; keep it optional by not blocking on it.
        // If really needed later, add to request and gate accordingly.
        // req.add(Manifest.permission.READ_PHONE_STATE);

        // Storage only if truly needed and per-SDK rules
        if (NEEDS_GALLERY_READ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // 33+
                req.add(Manifest.permission.READ_MEDIA_IMAGES);
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) { // ≤28
                req.add(Manifest.permission.READ_EXTERNAL_STORAGE);
                req.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        return req.toArray(new String[0]);
    }

    // Core gate: camera AND (coarse OR fine); phone state not required for this phase.
    public static boolean hasAllRequiredPermissions(Context ctx) {
        boolean camera = isGranted(ctx, Manifest.permission.CAMERA);
        boolean coarse = isGranted(ctx, Manifest.permission.ACCESS_COARSE_LOCATION);
        boolean fine = isGranted(ctx, Manifest.permission.ACCESS_FINE_LOCATION);

        // If gallery reads are enabled, add that here per-SDK
        if (NEEDS_GALLERY_READ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                boolean media = isGranted(ctx, Manifest.permission.READ_MEDIA_IMAGES);
                return camera && (coarse || fine) && media;
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                boolean read = isGranted(ctx, Manifest.permission.READ_EXTERNAL_STORAGE);
                boolean write = isGranted(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return camera && (coarse || fine) && read && write;
            }
        }

        return camera && (coarse || fine);
    }

    private static boolean isGranted(Context ctx, String p) {
        return ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED;
    }
}
