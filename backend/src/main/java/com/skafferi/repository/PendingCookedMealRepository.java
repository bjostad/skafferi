package com.skafferi.repository;

import com.skafferi.domain.PendingCookedMeal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PendingCookedMealRepository extends JpaRepository<PendingCookedMeal, String> {

    List<PendingCookedMeal> findByStatusOrderByCookedAtDesc(String status);

    long countByStatus(String status);
}
