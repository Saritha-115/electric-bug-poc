package com.aximly.electricbug.order.dao.impl;

import com.aximly.electricbug.order.dao.OrderDao;
import com.aximly.electricbug.order.dto.LaybyOrderDto;
import com.aximly.electricbug.order.dto.OrderDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Repository
@ConditionalOnProperty(name = "externalApiFlag", havingValue = "true")
public class AaaPosOrderDaoImpl implements OrderDao {

    private final WebClient aaaPosWebClient;

    public AaaPosOrderDaoImpl(WebClient aaaPosWebClient) {
        this.aaaPosWebClient = aaaPosWebClient;
    }

    @Override
    public List<OrderDto> getAllOrders() {
        // TODO: replace "/orders" with the real AAAPOS endpoint once confirmed,
        // and map its actual response shape into OrderDto below.
        return aaaPosWebClient.get()
                .uri("/orders")
                .retrieve()
                .bodyToFlux(OrderDto.class)
                .collectList()
                .block();
    }

    @Override
    public List<LaybyOrderDto> getLaybyOrders() {
        // TODO: replace "/orders/layby" with the real AAAPOS endpoint once confirmed
        return aaaPosWebClient.get()
                .uri("/orders/layby")
                .retrieve()
                .bodyToFlux(LaybyOrderDto.class)
                .collectList()
                .block();
    }
}