package com.profmojo.services;

import com.profmojo.models.Attendance;

import java.util.List;

public interface AttendanceService {

    void markAttendance(
            String classCode,
            String studentRegNo,
            boolean present
    );

    List<Attendance> getAttendanceForClassToday(String classCode);
}
