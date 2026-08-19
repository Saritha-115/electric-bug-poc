package com.aximly.electricbug.order.service.impl;

import com.aximly.electricbug.order.dao.OrderDao;
import com.aximly.electricbug.order.dto.LaybyOrderDto;
import com.aximly.electricbug.order.dto.OrderDto;
import com.aximly.electricbug.order.service.OrderService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderDao orderDao;

    public OrderServiceImpl(OrderDao orderDao) {
        this.orderDao = orderDao;
    }

    @Override
    public List<OrderDto> getAllOrders() {
        return orderDao.getAllOrders();
    }

    @Override
    public List<LaybyOrderDto> getLaybyOrders() {
        return orderDao.getLaybyOrders();
    }
}