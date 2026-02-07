package com.profmojo.repositories;

import com.profmojo.models.StaffReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StaffReportRepository extends JpaRepository<StaffReport, Long> {
    List<StaffReport> findByDepartmentAndResolvedFalse(String department);
    List<StaffReport> findByStaffId(String staffId);
    List<StaffReport> findByProfessorId(String professorId);
}