package com.profmojo.repositories;

import com.profmojo.models.CanteenFolder.CanteenMaster;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanteenMasterRepository
        extends JpaRepository<CanteenMaster, String> {
}
