package com.profmojo.services;

import com.profmojo.models.CanteenFolder.Canteen;

public interface CanteenService {

    Canteen registerCanteen(Canteen canteen);

    boolean existsByCanteenId(String canteenId);
}
