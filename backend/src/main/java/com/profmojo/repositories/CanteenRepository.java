package com.profmojo.repositories;

import com.profmojo.models.CanteenFolder.Canteen;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanteenRepository
        extends JpaRepository<Canteen, String> {
}
