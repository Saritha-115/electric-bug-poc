package com.aximly.electricbug.order.dao.impl;

import com.aximly.electricbug.order.dao.OrderDao;
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
        return queryLaybys("SELECT * FROM layby");
    }

    @Override
    public List<OrderDto> getLaybyOrders() {
        // in this schema, "layby orders" and "orders" are the same table —
        // adjust this query if/when a separate non-layby "orders" table exists
        return queryLaybys("SELECT * FROM layby WHERE closed = false");
    }

    private List<OrderDto> queryLaybys(String sql) {
        List<OrderDto> list = new ArrayList<>();
        try (Connection conn = cloudDataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                OrderDto dto = new OrderDto();
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
            throw new RuntimeException("Failed to read orders from Postgres: " + e.getMessage(), e);
        }
        return list;
    }
}