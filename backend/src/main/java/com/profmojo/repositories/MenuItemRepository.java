package com.profmojo.repositories;

import com.profmojo.models.CanteenFolder.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MenuItemRepository
        extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByCanteenId(String canteenId);

    List<MenuItem> findByCanteenIdAndAvailableTrue(String canteenId);
}

