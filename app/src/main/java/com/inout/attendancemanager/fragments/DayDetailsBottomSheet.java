package com.inout.attendancemanager.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.inout.attendancemanager.R;
import com.inout.attendancemanager.models.Attendance;
import com.inout.attendancemanager.utils.DateUtils;

import java.util.Date;

public class DayDetailsBottomSheet extends DialogFragment {

    private static final String ARG_DATE_ID = "dateId";
    private static final String ARG_IN_TIME = "inTime";
    private static final String ARG_OUT_TIME = "outTime";
    private static final String ARG_TOTAL_MIN = "totalMinutes";

    public static DayDetailsBottomSheet show(@NonNull FragmentManager fm, Attendance att) {
        DayDetailsBottomSheet sheet = new DayDetailsBottomSheet();
        Bundle args = new Bundle();
        args.putString(ARG_DATE_ID, att.getDateId());
        args.putLong(ARG_IN_TIME, att.getInTime() != null ? att.getInTime().getTime() : -1L);
        args.putLong(ARG_OUT_TIME, att.getOutTime() != null ? att.getOutTime().getTime() : -1L);
        args.putLong(ARG_TOTAL_MIN, att.getTotalMinutes());
        sheet.setArguments(args);
        sheet.show(fm, "DayDetailsBottomSheet");
        return sheet;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_day_details, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvTitle = view.findViewById(R.id.tv_sheet_title);
        TextView tvTotalWork = view.findViewById(R.id.tv_total_work);
        LinearLayout container = view.findViewById(R.id.container_sessions);

        Bundle args = getArguments();
        if (args == null) return;

        String dateId = args.getString(ARG_DATE_ID, "");
        long total = args.getLong(ARG_TOTAL_MIN, 0L);
        long inMillis = args.getLong(ARG_IN_TIME, -1L);
        long outMillis = args.getLong(ARG_OUT_TIME, -1L);

        tvTitle.setText("Details • " + dateId);
        tvTotalWork.setText("Total Worked: " + DateUtils.formatDuration(total));

        View sessionView = LayoutInflater.from(getContext()).inflate(R.layout.item_session_row, container, false);
        TextView tvIn = sessionView.findViewById(R.id.tv_in_time);
        TextView tvOut = sessionView.findViewById(R.id.tv_out_time);
        TextView tvDur = sessionView.findViewById(R.id.tv_session_duration);

        if (inMillis > 0) tvIn.setText(DateUtils.formatTime(new Date(inMillis)));
        else tvIn.setText("--:--");

        if (outMillis > 0) tvOut.setText(DateUtils.formatTime(new Date(outMillis)));
        else tvOut.setText("--:--");

        long dur = (inMillis > 0 && outMillis > 0) ? (outMillis - inMillis) / (1000 * 60) : 0;
        tvDur.setText(DateUtils.formatDuration(dur));

        container.addView(sessionView);
    }
}
