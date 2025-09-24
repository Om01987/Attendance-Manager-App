package com.inout.attendancemanager.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.inout.attendancemanager.R;
import com.inout.attendancemanager.models.Employee;

import java.util.ArrayList;
import java.util.List;

/**
 * AdminApprovalActivity
 * - Lists employees where approvalStatus == "pending"
 * - Admin can approve/reject each entry
 * - Uses Firestore addSnapshotListener; no FirebaseUI dependency needed
 */
public class AdminApprovalActivity extends AppCompatActivity {

    private RecyclerView rv;
    private FirebaseFirestore db;
    private EmployeesAdapter adapter;
    private final List<Employee> employees = new ArrayList<>();
    private final List<String> employeeIds = new ArrayList<>();
    private ListenerRegistration registration;

    @Override
    protected void onCreate(@Nullable Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_admin_approval);

        rv = findViewById(R.id.rv_employees);
        rv.setLayoutManager(new LinearLayoutManager(this));

        db = FirebaseFirestore.getInstance();

        adapter = new EmployeesAdapter(employees, new OnAction() {
            @Override public void onApprove(int position) {
                updateStatus(employeeIds.get(position), "approved");
            }
            @Override public void onReject(int position) {
                updateStatus(employeeIds.get(position), "rejected");
            }
        });
        rv.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        attachListener();
    }

    @Override
    protected void onStop() {
        super.onStop();
        detachListener();
    }

    private void attachListener() {
        if (registration != null) return;
        registration = db.collection("employees")
                .whereEqualTo("approvalStatus", "pending")
                .addSnapshotListener(this, new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException e) {
                        if (e != null) {
                            Toast.makeText(AdminApprovalActivity.this, "Load failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            return;
                        }
                        employees.clear();
                        employeeIds.clear();
                        if (snapshots != null) {
                            for (DocumentSnapshot doc : snapshots.getDocuments()) {
                                Employee emp = doc.toObject(Employee.class);
                                if (emp != null) {
                                    employees.add(emp);
                                    employeeIds.add(doc.getId());
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void detachListener() {
        if (registration != null) {
            registration.remove();
            registration = null;
        }
    }

    private void updateStatus(String uid, String status) {
        db.collection("employees").document(uid)
                .update("approvalStatus", status)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Updated to " + status, Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Callback interface for row actions
    interface OnAction {
        void onApprove(int position);
        void onReject(int position);
    }

    // RecyclerView Adapter without FirebaseUI
    static class EmployeesAdapter extends RecyclerView.Adapter<EmployeesAdapter.VH> {
        private final List<Employee> data;
        private final OnAction onAction;

        EmployeesAdapter(List<Employee> data, OnAction onAction) {
            this.data = data;
            this.onAction = onAction;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_employee_approval, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH h, int position) {
            Employee emp = data.get(position);
            h.tvName.setText(emp.getFullName() != null ? emp.getFullName() : "-");
            h.tvDepartment.setText(emp.getDepartment() != null ? emp.getDepartment() : "-");
            h.tvEmployeeId.setText(emp.getEmployeeId() != null ? emp.getEmployeeId() : "-");

            h.btnApprove.setOnClickListener(v -> onAction.onApprove(position));
            h.btnReject.setOnClickListener(v -> onAction.onReject(position));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvName, tvDepartment, tvEmployeeId;
            Button btnApprove, btnReject;

            VH(@NonNull View v) {
                super(v);
                tvName = v.findViewById(R.id.tv_name);
                tvDepartment = v.findViewById(R.id.tv_department);
                tvEmployeeId = v.findViewById(R.id.tv_employee_id);
                btnApprove = v.findViewById(R.id.btn_approve);
                btnReject = v.findViewById(R.id.btn_reject);
            }
        }
    }
}
