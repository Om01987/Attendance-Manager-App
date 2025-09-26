package com.inout.attendancemanager.utils;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.inout.attendancemanager.models.BeaconConfig;
import com.inout.attendancemanager.models.BeaconScanResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BeaconScanner {
    private static final String TAG = "BeaconScanner";
    private static final long SCAN_TIMEOUT_MS = 6000; // 6 seconds

    public interface ScanCallback {
        void onScanStarted();
        void onBeaconFound(BeaconScanResult result);
        void onScanComplete(BeaconScanResult bestResult);
        void onScanError(String error);
    }

    private final Context context;
    private final ScanCallback callback;
    private final List<BeaconConfig> beaconConfigs;

    private BluetoothLeScanner bleScanner;
    private Handler scanHandler;
    private Runnable scanTimeoutRunnable;
    private boolean isScanning = false;

    private Map<String, BeaconScanResult> foundBeacons = new HashMap<>();

    public BeaconScanner(Context context, List<BeaconConfig> beaconConfigs, ScanCallback callback) {
        this.context = context.getApplicationContext();
        this.beaconConfigs = beaconConfigs;
        this.callback = callback;
        this.scanHandler = new Handler(Looper.getMainLooper());
    }

    public void startScan() {
        if (isScanning) {
            Log.w(TAG, "Scan already in progress");
            return;
        }

        // Validate Bluetooth state
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            callback.onScanError("Bluetooth not available");
            return;
        }

        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled()) {
            callback.onScanError("Bluetooth is disabled");
            return;
        }

        // Check permissions
        if (!hasRequiredPermissions()) {
            callback.onScanError("Bluetooth scan permissions not granted");
            return;
        }

        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        if (bleScanner == null) {
            callback.onScanError("BLE scanner not available");
            return;
        }

        foundBeacons.clear();
        isScanning = true;
        callback.onScanStarted();

        // Configure scan settings for foreground scanning
        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        // Start scanning without filters to detect all beacons
        try {
            bleScanner.startScan(null, settings, bleScanCallback);

            // Set timeout
            scanTimeoutRunnable = this::stopScan;
            scanHandler.postDelayed(scanTimeoutRunnable, SCAN_TIMEOUT_MS);

            Log.d(TAG, "BLE scan started for " + SCAN_TIMEOUT_MS + "ms");
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception starting scan", e);
            isScanning = false;
            callback.onScanError("Permission denied for BLE scan");
        }
    }

    public void stopScan() {
        if (!isScanning) return;

        isScanning = false;

        if (scanTimeoutRunnable != null) {
            scanHandler.removeCallbacks(scanTimeoutRunnable);
            scanTimeoutRunnable = null;
        }

        if (bleScanner != null) {
            try {
                bleScanner.stopScan(bleScanCallback);
                Log.d(TAG, "BLE scan stopped");
            } catch (SecurityException e) {
                Log.e(TAG, "Security exception stopping scan", e);
            }
        }

        // Return best result
        BeaconScanResult bestResult = getBestResult();
        callback.onScanComplete(bestResult);
    }

    private final android.bluetooth.le.ScanCallback bleScanCallback = new android.bluetooth.le.ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            processScanResult(result);
        }

        @Override
        public void onScanFailed(int errorCode) {
            Log.e(TAG, "BLE scan failed with error code: " + errorCode);
            isScanning = false;
            callback.onScanError("Scan failed with code: " + errorCode);
        }
    };

    private void processScanResult(ScanResult result) {
        if (result.getScanRecord() == null) return;

        // Parse iBeacon format from manufacturer data
        byte[] manufacturerData = result.getScanRecord().getManufacturerSpecificData(0x004C); // Apple company ID
        if (manufacturerData != null && manufacturerData.length >= 23) {
            parseIBeacon(result, manufacturerData);
        }
    }

    private void parseIBeacon(ScanResult result, byte[] data) {
        // iBeacon format: [2 bytes type][16 bytes UUID][2 bytes Major][2 bytes Minor][1 byte TX Power]
        if (data[0] != 0x02 || data[1] != 0x15) return; // Not an iBeacon

        // Extract UUID
        byte[] uuidBytes = new byte[16];
        System.arraycopy(data, 2, uuidBytes, 0, 16);
        String uuid = parseUUID(uuidBytes).toString().toLowerCase();

        // Extract Major and Minor
        int major = ((data[18] & 0xFF) << 8) | (data[19] & 0xFF);
        int minor = ((data[20] & 0xFF) << 8) | (data[21] & 0xFF);

        // Check against configured beacons
        for (BeaconConfig config : beaconConfigs) {
            if (config.isEnabled() &&
                    uuid.equals(config.getUuid().toLowerCase()) &&
                    major == config.getMajor() &&
                    minor == config.getMinor()) {

                int rssi = result.getRssi();
                boolean isValid = rssi >= config.getRssiThreshold();

                BeaconScanResult scanResult = new BeaconScanResult(
                        config.getBeaconId(), rssi, config.getLabel(), isValid
                );

                // Keep the strongest signal for each beacon
                BeaconScanResult existing = foundBeacons.get(config.getBeaconId());
                if (existing == null || rssi > existing.getRssi()) {
                    foundBeacons.put(config.getBeaconId(), scanResult);
                    callback.onBeaconFound(scanResult);
                }

                Log.d(TAG, String.format("Found beacon %s: RSSI=%d, Valid=%b",
                        config.getLabel(), rssi, isValid));
                break;
            }
        }
    }

    private UUID parseUUID(byte[] bytes) {
        long mostSig = 0;
        long leastSig = 0;

        for (int i = 0; i < 8; i++) {
            mostSig = (mostSig << 8) | (bytes[i] & 0xFF);
        }
        for (int i = 8; i < 16; i++) {
            leastSig = (leastSig << 8) | (bytes[i] & 0xFF);
        }

        return new UUID(mostSig, leastSig);
    }

    private BeaconScanResult getBestResult() {
        BeaconScanResult best = null;

        for (BeaconScanResult result : foundBeacons.values()) {
            if (result.isValid() && (best == null || result.getRssi() > best.getRssi())) {
                best = result;
            }
        }

        return best != null ? best : BeaconScanResult.invalid();
    }

    private boolean hasRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.BLUETOOTH_SCAN)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }
}
