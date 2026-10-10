package com.skafferi.service;

import com.skafferi.domain.Item;
import com.skafferi.domain.PurchaseRecord;
import com.skafferi.dto.PurchaseRecordDto;
import com.skafferi.repository.ItemRepository;
import com.skafferi.repository.PurchaseRecordRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class PurchaseRecordService {

    private final PurchaseRecordRepository purchaseRecordRepository;
    private final ItemRepository itemRepository;

    public PurchaseRecordService(PurchaseRecordRepository purchaseRecordRepository, ItemRepository itemRepository) {
        this.purchaseRecordRepository = purchaseRecordRepository;
        this.itemRepository = itemRepository;
    }

    public List<PurchaseRecordDto> getPurchasesForItem(String itemId) {
        return purchaseRecordRepository.findByItemIdOrderByPurchasedDateDescCreatedAtDesc(itemId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public PurchaseRecordDto addPurchase(PurchaseRecordDto dto) {
        Item item = itemRepository.findById(dto.itemId())
                .orElseThrow(() -> new NoSuchElementException("Item not found: " + dto.itemId()));

        PurchaseRecord record = new PurchaseRecord();
        record.setItem(item);
        record.setPurchasedDate(dto.purchasedDate());
        record.setStore(dto.store());
        record.setQuantity(dto.quantity());
        record.setUnit(dto.unit() != null ? dto.unit() : item.getDefaultUnit());
        record.setUnitPrice(dto.unitPrice());
        if (dto.totalPrice() != null) {
            record.setTotalPrice(dto.totalPrice());
        } else if (dto.unitPrice() != null && dto.quantity() != null) {
            record.setTotalPrice(dto.unitPrice() * dto.quantity());
        }
        record.setNotes(dto.notes());
        record.setSource(dto.source() != null ? dto.source() : "MANUAL");

        PurchaseRecord saved = purchaseRecordRepository.save(record);
        return mapToDto(saved);
    }

    @Transactional
    public PurchaseRecord recordPurchase(Item item, LocalDate purchasedDate, String store, Double quantity, String unit, Double unitPrice, Double totalPrice, String notes, String source) {
        PurchaseRecord record = new PurchaseRecord();
        record.setItem(item);
        record.setPurchasedDate(purchasedDate);
        record.setStore(store);
        record.setQuantity(quantity);
        record.setUnit(unit != null ? unit : item.getDefaultUnit());
        record.setUnitPrice(unitPrice);
        if (totalPrice != null) {
            record.setTotalPrice(totalPrice);
        } else if (unitPrice != null && quantity != null) {
            record.setTotalPrice(unitPrice * quantity);
        }
        record.setNotes(notes);
        record.setSource(source != null ? source : "MANUAL");

        return purchaseRecordRepository.save(record);
    }

    @Transactional
    public void deletePurchase(String purchaseId) {
        purchaseRecordRepository.deleteById(purchaseId);
    }

    private PurchaseRecordDto mapToDto(PurchaseRecord p) {
        return new PurchaseRecordDto(
                p.getId(),
                p.getItem() != null ? p.getItem().getId() : null,
                p.getItem() != null ? p.getItem().getName() : null,
                p.getPurchasedDate(),
                p.getStore(),
                p.getQuantity(),
                p.getUnit(),
                p.getUnitPrice(),
                p.getTotalPrice(),
                p.getNotes(),
                p.getSource(),
                p.getCreatedAt()
        );
    }
}
