package com.profmojo.repositories;

import com.profmojo.models.Notice;
import com.profmojo.models.Professor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findByCreatedBy(Professor professor);

    List<Notice> findByCreatedByOrderByCreatedAtDesc(Professor professor);

}
