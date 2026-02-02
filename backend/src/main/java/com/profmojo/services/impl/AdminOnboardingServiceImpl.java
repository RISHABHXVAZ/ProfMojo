package com.profmojo.services.impl;

import com.opencsv.CSVReader;
import com.profmojo.models.ProfessorMaster;
import com.profmojo.models.Staff;
import com.profmojo.models.StudentMaster;
import com.profmojo.models.dto.AddStaffRequest;
import com.profmojo.repositories.ProfessorMasterRepository;
import com.profmojo.repositories.StaffRepository;
import com.profmojo.repositories.StudentMasterRepository;
import com.profmojo.services.AdminOnboardingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminOnboardingServiceImpl implements AdminOnboardingService {

    private final ProfessorMasterRepository professorMasterRepository;
    private final StudentMasterRepository studentMasterRepository;
    private final StaffRepository staffRepository;

    @Override
    public void addProfessor(ProfessorMaster professor) {
        if (professorMasterRepository.existsById(professor.getProfId())) {
            throw new RuntimeException("Professor already exists");
        }
        professorMasterRepository.save(professor);
    }

    @Override
    public Map<String, Object> addProfessorsFromCsv(MultipartFile file) {

        int added = 0;
        int skipped = 0;

        try (
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                CSVReader csvReader = new CSVReader(reader)
        ) {
            String[] row;
            boolean isHeader = true;

            while ((row = csvReader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String profId = row[0].trim();
                String name = row[1].trim();
                String department = row[2].trim();
                String email = row[3].trim();

                if (professorMasterRepository.existsById(profId)) {
                    skipped++;
                    continue;
                }

                ProfessorMaster professor = ProfessorMaster.builder()
                        .profId(profId)
                        .name(name)
                        .department(department)
                        .email(email)
                        .build();

                professorMasterRepository.save(professor);
                added++;
            }

        } catch (Exception e) {
            throw new RuntimeException("Invalid CSV file");
        }

        return Map.of(
                "added", added,
                "skipped", skipped,
                "message", "CSV processed successfully"
        );
    }

    @Override
    public void addStudent(StudentMaster student) {

        if (studentMasterRepository.existsById(student.getRegNo())) {
            throw new RuntimeException("Student already exists");
        }

        studentMasterRepository.save(student);
    }

    @Override
    public Map<String, Object> addStudentsFromCsv(MultipartFile file) {

        int added = 0;
        int skipped = 0;

        try (
                Reader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream())
                );
                CSVReader csvReader = new CSVReader(reader)
        ) {
            String[] row;
            boolean isHeader = true;

            while ((row = csvReader.readNext()) != null) {

                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String regNo = row[0].trim();
                String name = row[1].trim();
                String department = row[2].trim();
                String email = row[3].trim();

                if (studentMasterRepository.existsById(regNo)) {
                    skipped++;
                    continue;
                }

                StudentMaster student = StudentMaster.builder()
                        .regNo(regNo)
                        .name(name)
                        .department(department)
                        .email(email)
                        .build();

                studentMasterRepository.save(student);
                added++;
            }

        } catch (Exception e) {
            throw new RuntimeException("Invalid CSV file");
        }

        return Map.of(
                "added", added,
                "skipped", skipped,
                "message", "Student CSV processed successfully"
        );
    }

    @Override
    public void addStaff(AddStaffRequest req) {

        if (staffRepository.existsById(req.getStaffId())) {
            throw new RuntimeException("Staff already exists");
        }

        Staff staff = Staff.builder()
                .staffId(req.getStaffId())
                .name(req.getName())
                .department(req.getDepartment())
                .email(req.getEmail())
                .contactNo(req.getContactNo())
                .available(true)
                .online(false)
                .role("STAFF")
                .stars(0)
                .totalDeliveries(0)
                .build();

        staffRepository.save(staff);
    }


    @Override
    public Map<String, Object> addStaffFromCsv(MultipartFile file) {

        int added = 0;
        int skipped = 0;

        try (
                Reader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
                CSVReader csvReader = new CSVReader(reader)
        ) {
            String[] row;
            boolean isHeader = true;

            while ((row = csvReader.readNext()) != null) {
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String staffId = row[0].trim();
                String name = row[1].trim();
                String department = row[2].trim();
                String email = row[3].trim();
                String contactNo = row.length > 4 ? row[4].trim() : null;

                if (staffRepository.existsById(staffId)) {
                    skipped++;
                    continue;
                }

                Staff staff = Staff.builder()
                        .staffId(staffId)
                        .name(name)
                        .department(department)
                        .email(email)
                        .contactNo(contactNo)
                        .role("STAFF")
                        .available(true)
                        .online(false)
                        .build();

                staffRepository.save(staff);
                added++;
            }

        } catch (Exception e) {
            throw new RuntimeException("Invalid CSV file");
        }

        return Map.of(
                "added", added,
                "skipped", skipped,
                "message", "Staff CSV processed successfully"
        );
    }


}
