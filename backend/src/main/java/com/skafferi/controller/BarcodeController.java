package com.skafferi.controller;

import com.skafferi.dto.BarcodeLookupResult;
import com.skafferi.service.BarcodeLookupService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/barcode")
public class BarcodeController {

    private final BarcodeLookupService barcodeLookupService;

    public BarcodeController(BarcodeLookupService barcodeLookupService) {
        this.barcodeLookupService = barcodeLookupService;
    }

    @GetMapping("/{code}")
    public ResponseEntity<BarcodeLookupResult> lookupBarcode(@PathVariable String code) {
        BarcodeLookupResult result = barcodeLookupService.lookup(code);
        return ResponseEntity.ok(result);
    }
}
