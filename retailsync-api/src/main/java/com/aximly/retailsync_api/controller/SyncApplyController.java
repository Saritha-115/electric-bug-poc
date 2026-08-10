package com.aximly.retailsync_api.controller;

import com.aximly.retailsync_api.service.MdbApplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sync")
@RequiredArgsConstructor
public class SyncApplyController {

    private final MdbApplyService mdbApplyService;

    @Value("${sync.apply-secret}")
    private String applySecret;

    @PostMapping("/apply-pending-mdb-updates")
    public ResponseEntity<?> applyPendingUpdates(
            @RequestHeader("X-Apply-Secret") String secret,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        if (!applySecret.equals(secret)) return ResponseEntity.status(403).body("Forbidden");
        return ResponseEntity.ok(mdbApplyService.applyPendingUpdates(dryRun));
    }

    @PostMapping("/insert-layby-to-mdb/{laybyId}")
    public ResponseEntity<?> insertLaybyToMdb(@PathVariable int laybyId,
                                              @RequestParam(defaultValue = "true") boolean dryRun,
                                              @RequestHeader("X-Apply-Secret") String secret) {
        if (!secret.equals(applySecret)) {
            return ResponseEntity.status(403).body("Forbidden");
        }
        return ResponseEntity.ok(mdbApplyService.insertLaybyToMdb(laybyId, dryRun));
    }

    @PostMapping("/apply-pending-customer-updates")
    public ResponseEntity<?> applyPendingCustomerUpdates(
            @RequestHeader("X-Apply-Secret") String secret,
            @RequestParam(defaultValue = "true") boolean dryRun) {
        if (!applySecret.equals(secret)) return ResponseEntity.status(403).body("Forbidden");
        return ResponseEntity.ok(mdbApplyService.applyPendingCustomerUpdates(dryRun));
    }
}