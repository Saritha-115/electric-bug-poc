package com.aximly.retailsync_api.service;

import com.aximly.retailsync_api.MdbAccessLock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class MdbApplyService {

    @Value("${mdb.write-exe-path}")
    private String writeExePath;

    @Value("${mdb.file-path}")
    private String mdbFilePath;

    @Value("${mdb.password:}")
    private String mdbPassword;

    private final DataSource cloudDataSource;
    private final ObjectMapper mapper = new ObjectMapper();

    public MdbApplyService(@Qualifier("cloudDataSource") DataSource cloudDataSource) {
        this.cloudDataSource = cloudDataSource;
    }

    public record ApplyResult(int laybyId, boolean closed, boolean success, String message) {}

    public List<ApplyResult> applyPendingUpdates(boolean dryRun) {
        List<ApplyResult> results = new ArrayList<>();
        List<int[]> pending = new ArrayList<>();

        try (Connection cloudConn = cloudDataSource.getConnection();
             Statement stmt = cloudConn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT layby_id, closed FROM layby WHERE pending_mdb_sync = true")) {
            while (rs.next()) {
                pending.add(new int[]{ rs.getInt("layby_id"), rs.getBoolean("closed") ? 1 : 0 });
            }
        } catch (SQLException e) {
            results.add(new ApplyResult(-1, false, false, "Cloud read failed: " + e.getMessage()));
            return results;
        }

        for (int[] row : pending) {
            int laybyId = row[0];
            boolean closed = row[1] == 1;

            if (dryRun) {
                results.add(new ApplyResult(laybyId, closed, true, "DRY RUN — would set closed=" + closed));
                continue;
            }

            synchronized (MdbAccessLock.LOCK) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            writeExePath,
                            "layby",
                            mdbFilePath,
                            String.valueOf(laybyId),
                            String.valueOf(closed),
                            mdbPassword == null ? "" : mdbPassword
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) output.append(line);
                    }
                    boolean finished = process.waitFor(30, java.util.concurrent.TimeUnit.SECONDS);
                    if (!finished) {
                        process.destroyForcibly();
                        results.add(new ApplyResult(laybyId, closed, false, "mdb write timed out"));
                        continue;
                    }

                    JsonNode json = mapper.readTree(output.toString());
                    boolean success = json.path("success").asBoolean(false);
                    String message = json.path("message").asText("Unknown result");

                    if (success && json.path("updated").asInt(0) > 0) {
                        try (Connection cloudConn = cloudDataSource.getConnection();
                             PreparedStatement clear = cloudConn.prepareStatement(
                                     "UPDATE layby SET pending_mdb_sync = false WHERE layby_id = ?")) {
                            clear.setInt(1, laybyId);
                            clear.executeUpdate();
                        }
                    }
                    results.add(new ApplyResult(laybyId, closed, success, message));
                } catch (Exception e) {
                    results.add(new ApplyResult(laybyId, closed, false, "mdb write failed: " + e.getMessage()));
                }
            }
        }
        return results;
    }

    public ApplyResult insertLaybyToMdb(int laybyId, boolean dryRun) {
        // Fetch the layby row from cloud
        String laybyDateStr, comments;
        int customerId;
        double totalInc;
        boolean closed;

        try (Connection cloudConn = cloudDataSource.getConnection();
             PreparedStatement stmt = cloudConn.prepareStatement(
                     "SELECT layby_date, customer_id, total_inc, closed, comments FROM layby WHERE layby_id = ?")) {
            stmt.setInt(1, laybyId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return new ApplyResult(laybyId, false, false, "Layby not found in cloud");
                }
                laybyDateStr = rs.getTimestamp("layby_date").toString();
                customerId = rs.getInt("customer_id");
                totalInc = rs.getDouble("total_inc");
                closed = rs.getBoolean("closed");
                comments = rs.getString("comments");
            }
        } catch (SQLException e) {
            return new ApplyResult(laybyId, false, false, "Cloud read failed: " + e.getMessage());
        }

        if (dryRun) {
            return new ApplyResult(laybyId, closed, true, "DRY RUN — would insert layby " + laybyId);
        }

        synchronized (MdbAccessLock.LOCK) {
            try {
                ProcessBuilder pb = new ProcessBuilder(
                        writeExePath,
                        "layby-insert",
                        mdbFilePath,
                        String.valueOf(laybyId),
                        laybyDateStr,
                        String.valueOf(customerId),
                        String.valueOf(totalInc),
                        String.valueOf(closed),
                        comments == null ? "" : comments,
                        mdbPassword == null ? "" : mdbPassword
                );
                pb.redirectErrorStream(true);
                Process process = pb.start();

                StringBuilder output = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) output.append(line);
                }
                boolean finished = process.waitFor(30, TimeUnit.SECONDS);
                if (!finished) {
                    process.destroyForcibly();
                    return new ApplyResult(laybyId, closed, false, "mdb insert timed out");
                }

                JsonNode json = mapper.readTree(output.toString());
                boolean success = json.path("success").asBoolean(false);
                String message = json.path("message").asText("Unknown result");

                if (success && json.path("updated").asInt(0) > 0) {
                    try (Connection cloudConn = cloudDataSource.getConnection();
                         PreparedStatement clear = cloudConn.prepareStatement(
                                 "UPDATE layby SET pending_mdb_sync = false WHERE layby_id = ?")) {
                        clear.setInt(1, laybyId);
                        clear.executeUpdate();
                    }
                }
                return new ApplyResult(laybyId, closed, success, message);
            } catch (Exception e) {
                return new ApplyResult(laybyId, closed, false, "mdb insert failed: " + e.getMessage());
            }
        }
    }

    public record CustomerApplyResult(int customerId, boolean success, String message) {}

    public List<CustomerApplyResult> applyPendingCustomerUpdates(boolean dryRun) {
        List<CustomerApplyResult> results = new ArrayList<>();
        record PendingCustomer(int id, String givenNames, String surname, String email, String phone) {}
        List<PendingCustomer> pending = new ArrayList<>();

        try (Connection cloudConn = cloudDataSource.getConnection();
             Statement stmt = cloudConn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT customer_id, given_names, surname, email, phone FROM customer WHERE pending_mdb_sync = true")) {
            while (rs.next()) {
                pending.add(new PendingCustomer(
                        rs.getInt("customer_id"), rs.getString("given_names"),
                        rs.getString("surname"), rs.getString("email"), rs.getString("phone")));
            }
        } catch (SQLException e) {
            results.add(new CustomerApplyResult(-1, false, "Cloud read failed: " + e.getMessage()));
            return results;
        }

        for (PendingCustomer c : pending) {
            if (dryRun) {
                results.add(new CustomerApplyResult(c.id(), true, "DRY RUN — would update customer " + c.id()));
                continue;
            }

            synchronized (MdbAccessLock.LOCK) {
                try {
                    ProcessBuilder pb = new ProcessBuilder(
                            writeExePath, "customer", mdbFilePath,
                            String.valueOf(c.id()), c.givenNames(), c.surname(), c.email(), c.phone(),
                            mdbPassword == null ? "" : mdbPassword
                    );
                    pb.redirectErrorStream(true);
                    Process process = pb.start();

                    StringBuilder output = new StringBuilder();
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                        String line;
                        while ((line = reader.readLine()) != null) output.append(line);
                    }
                    process.waitFor(30, TimeUnit.SECONDS);

                    JsonNode json = mapper.readTree(output.toString());
                    boolean success = json.path("success").asBoolean(false);
                    String message = json.path("message").asText("Unknown result");

                    if (success && json.path("updated").asInt(0) > 0) {
                        try (Connection cloudConn = cloudDataSource.getConnection();
                             PreparedStatement clear = cloudConn.prepareStatement(
                                     "UPDATE customer SET pending_mdb_sync = false WHERE customer_id = ?")) {
                            clear.setInt(1, c.id());
                            clear.executeUpdate();
                        }
                    }
                    results.add(new CustomerApplyResult(c.id(), success, message));
                } catch (Exception e) {
                    results.add(new CustomerApplyResult(c.id(), false, "mdb write failed: " + e.getMessage()));
                }
            }
        }
        return results;
    }
}