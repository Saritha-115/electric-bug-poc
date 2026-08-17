package com.aximly.retailsync_api.service;

import com.aximly.retailsync_api.MdbAccessLock;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import javax.sql.DataSource;
import java.sql.*;

@Service
public class CloudSyncService {

    @Value("${spring.datasource.url}")
    private String mdbUrl;

    private final DataSource cloudDataSource;

    public CloudSyncService(@Qualifier("cloudDataSource") DataSource cloudDataSource) {
        this.cloudDataSource = cloudDataSource;
    }

    private Connection getMdbConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(mdbUrl);
        conn.setReadOnly(true);
        return conn;
    }

    // Runs every 10 minutes automatically
    @Scheduled(fixedRate = 10 * 60 * 1000) // back to 10 minutes
    public void scheduledSync() {
        syncNow();
    }

    // Public method — can be called on-demand (e.g. from a controller) OR by the scheduler above
    public void syncNow() {
        synchronized (MdbAccessLock.LOCK) {
            syncStock();
            syncCustomers();
            syncLaybys();
            syncPayments();
        }
    }

    private void syncStock() {
        String selectSql = "SELECT stock_id, Barcode, description, sell, cost, quantity, dept_id FROM Stock";
        String upsertSql = """
            INSERT INTO stock (stock_id, barcode, description, sell_price, cost_price, quantity, dept_id, synced_at)
            VALUES (?, ?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (stock_id) DO UPDATE SET
                barcode = EXCLUDED.barcode,
                description = EXCLUDED.description,
                sell_price = EXCLUDED.sell_price,
                cost_price = EXCLUDED.cost_price,
                quantity = EXCLUDED.quantity,
                dept_id = EXCLUDED.dept_id,
                synced_at = now()
            """;

        try (Connection mdbConn = getMdbConnection();
             Connection cloudConn = cloudDataSource.getConnection();
             Statement stmt = mdbConn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement ps = cloudConn.prepareStatement(upsertSql)) {

            int count = 0;
            while (rs.next()) {
                ps.setDouble(1, rs.getDouble("stock_id"));
                ps.setString(2, rs.getString("Barcode"));
                ps.setString(3, rs.getString("description"));
                ps.setDouble(4, rs.getDouble("sell"));
                ps.setDouble(5, rs.getDouble("cost"));
                ps.setDouble(6, rs.getDouble("quantity"));
                ps.setInt(7, rs.getInt("dept_id"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            System.out.println("[CloudSyncService] Synced " + count + " stock rows");
        } catch (SQLException e) {
            System.out.println("[CloudSyncService] Stock sync failed: " + e.getMessage());
        }
    }

    private void syncCustomers() {
        String selectSql = "SELECT customer_id, given_names, surname, email, phone FROM Customer";
        String upsertSql = """
            INSERT INTO customer (customer_id, given_names, surname, email, phone, synced_at)
            VALUES (?, ?, ?, ?, ?, now())
            ON CONFLICT (customer_id) DO UPDATE SET
                given_names = EXCLUDED.given_names,
                surname = EXCLUDED.surname,
                email = EXCLUDED.email,
                phone = EXCLUDED.phone,
                synced_at = now()
            """;

        try (Connection mdbConn = getMdbConnection();
             Connection cloudConn = cloudDataSource.getConnection();
             Statement stmt = mdbConn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement ps = cloudConn.prepareStatement(upsertSql)) {

            int count = 0;
            while (rs.next()) {
                ps.setInt(1, rs.getInt("customer_id"));
                ps.setString(2, rs.getString("given_names"));
                ps.setString(3, rs.getString("surname"));
                ps.setString(4, rs.getString("email"));
                ps.setString(5, rs.getString("phone"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            System.out.println("[CloudSyncService] Synced " + count + " customer rows");
        } catch (SQLException e) {
            System.out.println("[CloudSyncService] Customer sync failed: " + e.getMessage());
        }
    }

    private void syncLaybys() {
        String selectSql = "SELECT layby_id, layby_date, customer_id, total_inc, closed, comments FROM layby";
        String upsertSql = """
            INSERT INTO layby (layby_id, layby_date, customer_id, total_inc, closed, comments, synced_at)
            VALUES (?, ?, ?, ?, ?, ?, now())
            ON CONFLICT (layby_id) DO UPDATE SET
                layby_date = EXCLUDED.layby_date,
                customer_id = EXCLUDED.customer_id,
                total_inc = EXCLUDED.total_inc,
                closed = EXCLUDED.closed,
                comments = EXCLUDED.comments,
                synced_at = now()
            """;

        try (Connection mdbConn = getMdbConnection();
             Connection cloudConn = cloudDataSource.getConnection();
             Statement stmt = mdbConn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement ps = cloudConn.prepareStatement(upsertSql)) {

            int count = 0;
            while (rs.next()) {
                ps.setInt(1, rs.getInt("layby_id"));
                Timestamp laybyDate = rs.getTimestamp("layby_date");
                ps.setTimestamp(2, laybyDate);
                ps.setInt(3, rs.getInt("customer_id"));
                ps.setDouble(4, rs.getDouble("total_inc"));
                ps.setBoolean(5, rs.getBoolean("closed"));
                ps.setString(6, rs.getString("comments"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            System.out.println("[CloudSyncService] Synced " + count + " layby rows");
        } catch (SQLException e) {
            System.out.println("[CloudSyncService] Layby sync failed: " + e.getMessage());
        }
    }

    private void syncPayments() {
        String selectSql = "SELECT payment_id, docket_id, docket_date, paymenttype, amount FROM Payments";
        String upsertSql = """
            INSERT INTO payments (payment_id, docket_id, docket_date, paymenttype, amount, synced_at)
            VALUES (?, ?, ?, ?, ?, now())
            ON CONFLICT (payment_id) DO UPDATE SET
                docket_id = EXCLUDED.docket_id,
                docket_date = EXCLUDED.docket_date,
                paymenttype = EXCLUDED.paymenttype,
                amount = EXCLUDED.amount,
                synced_at = now()
            """;

        try (Connection mdbConn = getMdbConnection();
             Connection cloudConn = cloudDataSource.getConnection();
             Statement stmt = mdbConn.createStatement();
             ResultSet rs = stmt.executeQuery(selectSql);
             PreparedStatement ps = cloudConn.prepareStatement(upsertSql)) {

            int count = 0;
            while (rs.next()) {
                ps.setInt(1, rs.getInt("payment_id"));
                ps.setInt(2, rs.getInt("docket_id"));
                Timestamp docketDate = rs.getTimestamp("docket_date");
                ps.setTimestamp(3, docketDate);
                ps.setString(4, rs.getString("paymenttype"));
                ps.setDouble(5, rs.getDouble("amount"));
                ps.addBatch();
                count++;
            }
            ps.executeBatch();
            System.out.println("[CloudSyncService] Synced " + count + " payment rows");
        } catch (SQLException e) {
            System.out.println("[CloudSyncService] Payment sync failed: " + e.getMessage());
        }
    }
}