package com.aximly.retailsync_api.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

@Service
public class SyncService {

    private final DataSource cloudDataSource;

    public SyncService(@Qualifier("cloudDataSource") DataSource cloudDataSource) {
        this.cloudDataSource = cloudDataSource;
    }

    private Connection getCloudConnection() throws SQLException {
        return cloudDataSource.getConnection();
    }

    // ────────────────────────────────────────────────
    // READ / REPORTING — this is what the future Lambda API calls
    // ────────────────────────────────────────────────

    public List<Map<String, Object>> getAllLaybys() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getCloudConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM layby")) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("layby_id", rs.getInt("layby_id"));
                row.put("layby_date", rs.getTimestamp("layby_date"));
                row.put("customer_id", rs.getInt("customer_id"));
                row.put("total_inc", rs.getDouble("total_inc"));
                row.put("closed", rs.getBoolean("closed"));
                row.put("comments", rs.getString("comments"));
                list.add(row);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getOpenLaybys() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getCloudConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM layby WHERE closed = false")) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("layby_id", rs.getInt("layby_id"));
                row.put("layby_date", rs.getTimestamp("layby_date"));
                row.put("customer_id", rs.getInt("customer_id"));
                row.put("total_inc", rs.getDouble("total_inc"));
                row.put("closed", rs.getBoolean("closed"));
                list.add(row);
            }
        }
        return list;
    }

    public List<Map<String, Object>> getPaymentsForLayby(Integer laybyId) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getCloudConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM payments WHERE docket_id = ?")) {
            stmt.setInt(1, laybyId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("payment_id", rs.getInt("payment_id"));
                    row.put("docket_id", rs.getInt("docket_id"));
                    row.put("payment_type", rs.getString("paymenttype"));
                    row.put("amount", rs.getDouble("amount"));
                    row.put("date", rs.getTimestamp("docket_date"));
                    list.add(row);
                }
            }
        }
        return list;
    }

    public Map<String, Object> getRemainingBalance(Integer laybyId) throws SQLException {
        double total = 0;
        double paid = 0;
        try (Connection conn = getCloudConnection()) {
            try (PreparedStatement ls = conn.prepareStatement(
                    "SELECT total_inc FROM layby WHERE layby_id = ?")) {
                ls.setInt(1, laybyId);
                try (ResultSet lr = ls.executeQuery()) {
                    if (lr.next()) total = lr.getDouble("total_inc");
                }
            }
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT SUM(amount) as paid FROM payments WHERE docket_id = ?")) {
                ps.setInt(1, laybyId);
                try (ResultSet pr = ps.executeQuery()) {
                    if (pr.next()) paid = pr.getDouble("paid");
                }
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("layby_id", laybyId);
        result.put("total", total);
        result.put("paid", paid);
        result.put("balance", total - paid);
        return result;
    }

    public List<Map<String, Object>> getAllCustomers() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        try (Connection conn = getCloudConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM customer")) {
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("customer_id", rs.getInt("customer_id"));
                row.put("given_names", rs.getString("given_names"));
                row.put("surname", rs.getString("surname"));
                row.put("email", rs.getString("email"));
                row.put("phone", rs.getString("phone"));
                list.add(row);
            }
        }
        return list;
    }

    public void updateCustomer(int id, String givenNames, String surname, String email, String phone) throws SQLException {
        try (Connection conn = getCloudConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE customer SET given_names = ?, surname = ?, email = ?, phone = ?, pending_mdb_sync = true WHERE customer_id = ?")) {
            ps.setString(1, givenNames);
            ps.setString(2, surname);
            ps.setString(3, email);
            ps.setString(4, phone);
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public void updateLaybyStatus(int id, boolean closed) throws SQLException {
        try (Connection conn = getCloudConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "UPDATE layby SET closed = ?, pending_mdb_sync = true WHERE layby_id = ?")) {
            ps.setBoolean(1, closed);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }
}