package com.skafferi.service;

import com.skafferi.domain.Item;
import com.skafferi.domain.PurchaseRecord;
import com.skafferi.dto.PurchaseRecordDto;
import com.skafferi.repository.ItemRepository;
import com.skafferi.repository.PurchaseRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class PurchaseRecordServiceTest {

    private PurchaseRecordRepository purchaseRecordRepository;
    private ItemRepository itemRepository;
    private PurchaseRecordService purchaseRecordService;

    @BeforeEach
    void setUp() {
        purchaseRecordRepository = mock(PurchaseRecordRepository.class);
        itemRepository = mock(ItemRepository.class);
        purchaseRecordService = new PurchaseRecordService(purchaseRecordRepository, itemRepository);
    }

    @Test
    void testAddPurchaseWithOptionalFieldsNull() {
        Item item = new Item();
        item.setId("item-123");
        item.setName("Whole Milk");
        item.setDefaultUnit("gal");

        when(itemRepository.findById("item-123")).thenReturn(Optional.of(item));
        when(purchaseRecordRepository.save(any(PurchaseRecord.class))).thenAnswer(invocation -> {
            PurchaseRecord r = invocation.getArgument(0);
            r.setId("rec-1");
            return r;
        });

        // None of the fields are required: test null price, null store, null quantity, null date
        PurchaseRecordDto input = new PurchaseRecordDto(
                null,
                "item-123",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        PurchaseRecordDto result = purchaseRecordService.addPurchase(input);

        assertNotNull(result);
        assertEquals("rec-1", result.id());
        assertEquals("item-123", result.itemId());
        assertNull(result.store());
        assertNull(result.unitPrice());
        assertNull(result.totalPrice());
        assertNull(result.quantity());

        ArgumentCaptor<PurchaseRecord> captor = ArgumentCaptor.forClass(PurchaseRecord.class);
        verify(purchaseRecordRepository).save(captor.capture());
        PurchaseRecord saved = captor.getValue();
        assertEquals(item, saved.getItem());
        assertNull(saved.getStore());
        assertNull(saved.getUnitPrice());
    }

    @Test
    void testRecordPurchaseFullFields() {
        Item item = new Item();
        item.setId("item-456");
        item.setName("Avocados");
        item.setDefaultUnit("count");

        when(purchaseRecordRepository.save(any(PurchaseRecord.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LocalDate date = LocalDate.of(2026, 10, 4);
        PurchaseRecord record = purchaseRecordService.recordPurchase(
                item,
                date,
                "Fred Meyer",
                4.0,
                "count",
                1.25,
                5.00,
                "Sale discount",
                "RECEIPT"
        );

        assertEquals("Fred Meyer", record.getStore());
        assertEquals(date, record.getPurchasedDate());
        assertEquals(4.0, record.getQuantity());
        assertEquals(1.25, record.getUnitPrice());
        assertEquals(5.00, record.getTotalPrice());
        assertEquals("Sale discount", record.getNotes());
        assertEquals("RECEIPT", record.getSource());
    }

    @Test
    void testGetPurchasesForItem() {
        Item item = new Item();
        item.setId("item-1");
        item.setName("Eggs");

        PurchaseRecord r1 = new PurchaseRecord();
        r1.setId("p1");
        r1.setItem(item);
        r1.setStore("Costco");
        r1.setPurchasedDate(LocalDate.now());
        r1.setUnitPrice(4.50);

        when(purchaseRecordRepository.findByItemIdOrderByPurchasedDateDescCreatedAtDesc("item-1"))
                .thenReturn(List.of(r1));

        List<PurchaseRecordDto> list = purchaseRecordService.getPurchasesForItem("item-1");
        assertEquals(1, list.size());
        assertEquals("Costco", list.get(0).store());
        assertEquals(4.50, list.get(0).unitPrice());
    }
}
