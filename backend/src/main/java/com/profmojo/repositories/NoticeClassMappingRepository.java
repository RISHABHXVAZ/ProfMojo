package com.profmojo.repositories;

import com.profmojo.models.Notice;
import com.profmojo.models.NoticeClassMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeClassMappingRepository extends JpaRepository<NoticeClassMapping, Long> {
    List<NoticeClassMapping> findByClassCodeIn(List<String> classCodes);

    List<NoticeClassMapping> findByNotice(Notice notice);
}
