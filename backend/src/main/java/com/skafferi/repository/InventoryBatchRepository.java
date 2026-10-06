package com.skafferi.repository;

import com.skafferi.domain.InventoryBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, String> {

    List<InventoryBatch> findByItemIdOrderByExpirationDateAsc(String itemId);

    List<InventoryBatch> findByLocationId(String locationId);

    List<InventoryBatch> findByQuantityGreaterThan(double minQuantity);

    @Query("SELECT b FROM InventoryBatch b WHERE b.expirationDate IS NOT NULL AND b.expirationDate <= :date AND b.quantity > 0 ORDER BY b.expirationDate ASC")
    List<InventoryBatch> findExpiringBefore(@Param("date") LocalDate date);

    @Query("SELECT COALESCE(SUM(b.quantity), 0.0) FROM InventoryBatch b WHERE b.item.id = :itemId")
    Double getTotalQuantityForItem(@Param("itemId") String itemId);
}
