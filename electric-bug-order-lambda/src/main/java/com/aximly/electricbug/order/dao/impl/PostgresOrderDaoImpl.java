package com.aximly.electricbug.order.dao.impl;

import com.aximly.electricbug.order.dao.OrderDao;
import com.aximly.electricbug.order.dto.LaybyOrderDto;
import com.aximly.electricbug.order.dto.OrderDto;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Repository
@ConditionalOnProperty(name = "externalApiFlag", havingValue = "false", matchIfMissing = true)
public class PostgresOrderDaoImpl implements OrderDao {

    private final DataSource cloudDataSource;

    public PostgresOrderDaoImpl(@Qualifier("cloudDataSource") DataSource cloudDataSource) {
        this.cloudDataSource = cloudDataSource;
    }

    @Override
    public List<OrderDto> getAllOrders() {
        List<OrderDto> list = new ArrayList<>();
        try (Connection conn = cloudDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM orders")) {
            while (rs.next()) {
                OrderDto dto = new OrderDto();
                dto.setOrderId(rs.getInt("order_id"));
                dto.setRevision((int) rs.getShort("revision"));
                dto.setOrderDate(rs.getTimestamp("order_date") != null
                        ? rs.getTimestamp("order_date").toLocalDateTime() : null);
                dto.setDueDate(rs.getTimestamp("due_date") != null
                        ? rs.getTimestamp("due_date").toLocalDateTime() : null);
                dto.setStaffId(rs.getInt("staff_id"));
                dto.setSupplierId(rs.getInt("supplier_id"));
                dto.setOrderSuffix(rs.getString("order_suffix"));
                dto.setComments(rs.getString("comments"));
                dto.setArchive(rs.getBoolean("archive"));
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read orders from Postgres: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<LaybyOrderDto> getLaybyOrders() {
        List<LaybyOrderDto> list = new ArrayList<>();
        try (Connection conn = cloudDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM layby")) {
            while (rs.next()) {
                LaybyOrderDto dto = new LaybyOrderDto();
                dto.setLaybyId(rs.getInt("layby_id"));
                dto.setLaybyDate(rs.getTimestamp("layby_date") != null
                        ? rs.getTimestamp("layby_date").toLocalDateTime() : null);
                dto.setCustomerId(rs.getInt("customer_id"));
                dto.setTotalInc(rs.getDouble("total_inc"));
                dto.setClosed(rs.getBoolean("closed"));
                dto.setComments(rs.getString("comments"));
                list.add(dto);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to read layby orders from Postgres: " + e.getMessage(), e);
        }
        return list;
    }
}