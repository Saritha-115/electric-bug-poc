package com.aximly.retailsync_api.controller;

import com.aximly.retailsync_api.service.CloudSyncService;
import com.aximly.retailsync_api.service.SyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.sql.SQLException;
import java.util.*;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SyncController {

    private final SyncService syncService;
    private final CloudSyncService cloudSyncService;

    // pull fresh data from .mdb into cloud Postgres, right now
    @PostMapping("/sync/refresh-cloud")
    public ResponseEntity<?> refreshCloud() {
        cloudSyncService.syncNow();
        return ResponseEntity.ok(Map.of("status", "cloud refreshed from .mdb"));
    }

    @GetMapping("/laybys")
    public ResponseEntity<?> getAllLaybys() throws SQLException {
        return ResponseEntity.ok(syncService.getAllLaybys());
    }

    @GetMapping("/laybys/open")
    public ResponseEntity<?> getOpenLaybys() throws SQLException {
        return ResponseEntity.ok(syncService.getOpenLaybys());
    }

    @GetMapping("/laybys/{id}/payments")
    public ResponseEntity<?> getPayments(@PathVariable Integer id) throws SQLException {
        return ResponseEntity.ok(syncService.getPaymentsForLayby(id));
    }

    @GetMapping("/laybys/{id}/balance")
    public ResponseEntity<?> getBalance(@PathVariable Integer id) throws SQLException {
        return ResponseEntity.ok(syncService.getRemainingBalance(id));
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "retailsync-api"));
    }
    @GetMapping("/customers")
    public ResponseEntity<?> getAllCustomers() throws SQLException {
        return ResponseEntity.ok(syncService.getAllCustomers());
    }

    @PatchMapping("/customers/{id}")
    public ResponseEntity<?> updateCustomer(@PathVariable Integer id, @RequestBody Map<String, String> body) throws SQLException {
        syncService.updateCustomer(id, body.get("given_names"), body.get("surname"), body.get("email"), body.get("phone"));
        return ResponseEntity.ok(Map.of("status", "queued for mdb sync"));
    }

    @PatchMapping("/laybys/{id}")
    public ResponseEntity<?> updateLaybyStatus(@PathVariable Integer id, @RequestBody Map<String, Boolean> body) throws SQLException {
        syncService.updateLaybyStatus(id, body.get("closed"));
        return ResponseEntity.ok(Map.of("status", "queued for mdb sync"));
    }
}