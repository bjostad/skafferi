package com.skafferi.repository;

import com.skafferi.domain.PurchaseRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseRecordRepository extends JpaRepository<PurchaseRecord, String> {

    List<PurchaseRecord> findByItemIdOrderByPurchasedDateDescCreatedAtDesc(String itemId);

    void deleteByItemId(String itemId);
}
