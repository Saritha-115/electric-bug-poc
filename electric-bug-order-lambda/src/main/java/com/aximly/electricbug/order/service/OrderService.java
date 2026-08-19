package com.aximly.electricbug.order.service;

import com.aximly.electricbug.order.dto.LaybyOrderDto;
import com.aximly.electricbug.order.dto.OrderDto;

import java.util.List;

public interface OrderService {
    List<OrderDto> getAllOrders();
    List<LaybyOrderDto> getLaybyOrders();
}