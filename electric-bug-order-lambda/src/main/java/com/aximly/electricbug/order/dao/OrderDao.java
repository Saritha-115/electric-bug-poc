package com.aximly.electricbug.order.dao;

import com.aximly.electricbug.order.dto.LaybyOrderDto;
import com.aximly.electricbug.order.dto.OrderDto;

import java.util.List;

public interface OrderDao {
    List<OrderDto> getAllOrders();
    List<LaybyOrderDto> getLaybyOrders();
}