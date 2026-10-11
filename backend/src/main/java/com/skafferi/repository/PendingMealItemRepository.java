package com.skafferi.repository;

import com.skafferi.domain.PendingMealItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PendingMealItemRepository extends JpaRepository<PendingMealItem, String> {

    List<PendingMealItem> findByMealId(String mealId);
}
