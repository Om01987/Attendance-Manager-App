package com.inout.attendancemanager.repositories;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import com.inout.attendancemanager.models.Attendance;
import com.inout.attendancemanager.utils.DateUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles attendance reads and writes with Firestore transactions.
 */
public class AttendanceRepository {
    private static final String TAG = "AttendanceRepo";

    private final FirebaseFirestore db;
    private final String userId;

    public AttendanceRepository(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    /** Observes today’s attendance document in real time. */
    public LiveData<Attendance> observeToday() {
        MutableLiveData<Attendance> live = new MutableLiveData<>();
        String dateId = DateUtils.getTodayDateId();
        db.collection("attendance")
                .document(userId)
                .collection("days")
                .document(dateId)
                .addSnapshotListener((snap, e) -> {
                    if (e != null) {
                        live.setValue(null);
                        return;
                    }
                    if (snap != null && snap.exists()) {
                        Attendance att = snap.toObject(Attendance.class);
                        att.setDateId(dateId);
                        live.setValue(att);
                    } else {
                        live.setValue(null);
                    }
                });
        return live;
    }

    public Task<Void> punchIn(String deviceId, Double lat, Double lng) {
        String dateId = DateUtils.getTodayDateId();
        DocumentReference docRef = db.collection("attendance")
                .document(userId)
                .collection("days")
                .document(dateId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            Map<String, Object> updates = new HashMap<>();
            updates.put("userId", userId);
            updates.put("dateId", dateId);
            updates.put("deviceId", deviceId);
            updates.put("method", "manual");
            updates.put("updatedAt", FieldValue.serverTimestamp());
            updates.put("status", "present_in_progress");

            if (!snap.exists()) {
                // First punch of the day
                updates.put("createdAt", FieldValue.serverTimestamp());
                updates.put("inTime", FieldValue.serverTimestamp());
                updates.put("outTime", null);
                updates.put("totalMinutes", 0L);
                if (lat != null && lng != null) {
                    updates.put("latitude", lat);
                    updates.put("longitude", lng);
                }
                transaction.set(docRef, updates, SetOptions.merge());
                return null;
            }

            Attendance old = snap.toObject(Attendance.class);
            // If currently in a session (outTime == null), prevent double punch-in
            if (old.getInTime() != null && old.getOutTime() == null) {
                throw new FirebaseFirestoreException("Already punched in",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            // Start a new session: set inTime=now, clear outTime
            updates.put("inTime", FieldValue.serverTimestamp());
            updates.put("outTime", null);
            if (lat != null && lng != null) {
                updates.put("latitude", lat);
                updates.put("longitude", lng);
            }
            transaction.update(docRef, updates);
            return null;
        });
    }

    public Task<Void> punchOut(String deviceId, Double lat, Double lng) {
        String dateId = DateUtils.getTodayDateId();
        DocumentReference docRef = db.collection("attendance")
                .document(userId)
                .collection("days")
                .document(dateId);

        return db.runTransaction(transaction -> {
            DocumentSnapshot snap = transaction.get(docRef);
            if (!snap.exists()) {
                throw new FirebaseFirestoreException("No punch-in record",
                        FirebaseFirestoreException.Code.NOT_FOUND);
            }
            Attendance old = snap.toObject(Attendance.class);
            if (old.getInTime() == null) {
                throw new FirebaseFirestoreException("No active session",
                        FirebaseFirestoreException.Code.FAILED_PRECONDITION);
            }
            if (old.getOutTime() != null) {
                // Already punched out last session
                throw new FirebaseFirestoreException("Already punched out",
                        FirebaseFirestoreException.Code.ABORTED);
            }

            Timestamp now = Timestamp.now();
            long deltaMillis = now.toDate().getTime() - old.getInTime().getTime();
            long prevMinutes = old.getTotalMinutes();
            long newTotalMinutes = prevMinutes + (deltaMillis / (1000 * 60));

            Map<String, Object> updates = new HashMap<>();
            updates.put("outTime", now);
            updates.put("updatedAt", now);
            updates.put("totalMinutes", newTotalMinutes);
            updates.put("deviceId", deviceId);
            if (lat != null && lng != null) {
                updates.put("latitude", lat);
                updates.put("longitude", lng);
            }

            // Shift target 9h = 540 min
            if (newTotalMinutes >= 540) {
                updates.put("status", "present_complete");
            } else {
                updates.put("status", "present_in_progress");
            }
            transaction.update(docRef, updates);
            return null;
        });
    }

}
