package com.inout.attendancemanager.models;

public class AttendanceSummary {
    private int presentDays;
    private int absentDays;
    private int weekoffDays;
    private int missedPunchDays;
    private long totalWorkingMinutes;
    private String month; // yyyy-MM format

    // Empty constructor
    public AttendanceSummary() {}

    // Getters and Setters
    public int getPresentDays() { return presentDays; }
    public void setPresentDays(int presentDays) { this.presentDays = presentDays; }

    public int getAbsentDays() { return absentDays; }
    public void setAbsentDays(int absentDays) { this.absentDays = absentDays; }

    public int getWeekoffDays() { return weekoffDays; }
    public void setWeekoffDays(int weekoffDays) { this.weekoffDays = weekoffDays; }

    public int getMissedPunchDays() { return missedPunchDays; }
    public void setMissedPunchDays(int missedPunchDays) { this.missedPunchDays = missedPunchDays; }

    public long getTotalWorkingMinutes() { return totalWorkingMinutes; }
    public void setTotalWorkingMinutes(long totalWorkingMinutes) { this.totalWorkingMinutes = totalWorkingMinutes; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
}
