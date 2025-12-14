package com.profmojo.controllers;

import com.profmojo.models.Attendance;
import com.profmojo.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    public void markAttendance(@RequestBody Map<String, String> req) {

        attendanceService.markAttendance(
                req.get("classCode"),
                req.get("studentRegNo"),
                Boolean.parseBoolean(req.get("present"))
        );
    }

    @GetMapping("/{classCode}/today")
    public List<Attendance> todayAttendance(@PathVariable String classCode) {
        return attendanceService.getAttendanceForClassToday(classCode);
    }
}
