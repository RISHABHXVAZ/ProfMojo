package com.profmojo.controllers;

import com.profmojo.models.Attendance;
import com.profmojo.models.dto.AttendanceRequest;
import com.profmojo.services.AttendanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
@CrossOrigin("http://localhost:5173")
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/mark")
    public ResponseEntity<?> markAttendance(@RequestBody AttendanceRequest request) {
        attendanceService.markAttendance(
                request.getClassCode(),
                request.getStudentRegNo(),
                request.isPresent()
        );
        return ResponseEntity.ok("Attendance saved");
    }

    @GetMapping("/{classCode}/today")
    public List<Attendance> todayAttendance(@PathVariable String classCode) {
        return attendanceService.getAttendanceForClassToday(classCode);
    }

    @GetMapping("/{classCode}/date")
    public List<Attendance> attendanceByDate(
            @PathVariable String classCode,
            @RequestParam String date
    ) {
        return attendanceService.getAttendanceForClassByDate(
                classCode,
                LocalDate.parse(date)
        );
    }

    @GetMapping("/{classCode}/student/{regNo}")
    public List<Attendance> studentAttendance(
            @PathVariable String classCode,
            @PathVariable String regNo
    ) {
        return attendanceService.getStudentAttendanceHistory(classCode, regNo);
    }


}
