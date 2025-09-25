package com.inout.attendancemanager.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.inout.attendancemanager.R;
import com.inout.attendancemanager.models.Employee;

import java.util.List;

public class AdminApprovalAdapter
        extends RecyclerView.Adapter<AdminApprovalAdapter.VH> {

    public interface OnStatusChange {
        void invoke(Employee employee, String newStatus);
    }

    private final List<Employee> list;
    private final OnStatusChange callback;

    public AdminApprovalAdapter(List<Employee> list, OnStatusChange cb) {
        this.list = list;
        this.callback = cb;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_employee_approval, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int pos) {
        Employee e = list.get(pos);
        holder.tvName.setText(e.getFullName());
        holder.tvDepartment.setText(e.getDepartment());
        holder.tvEmployeeId.setText(e.getEmployeeId());
        holder.btnApprove.setOnClickListener(v -> callback.invoke(e, "approved"));
        holder.btnReject.setOnClickListener(v -> callback.invoke(e, "rejected"));
    }

    @Override public int getItemCount() { return list.size(); }

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
