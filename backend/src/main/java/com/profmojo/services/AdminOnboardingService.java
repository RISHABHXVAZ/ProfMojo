package com.profmojo.services;

import com.profmojo.models.ProfessorMaster;
import com.profmojo.models.Staff;
import com.profmojo.models.StudentMaster;
import com.profmojo.models.dto.AddStaffRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface AdminOnboardingService {
    void addProfessor(ProfessorMaster professor);
    Map<String, Object> addProfessorsFromCsv(MultipartFile file);

    void addStudent(StudentMaster student);
    Map<String, Object> addStudentsFromCsv(MultipartFile file);

    void addStaff(AddStaffRequest req);
    Map<String, Object> addStaffFromCsv(MultipartFile file);

}
