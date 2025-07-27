package com.gradlehigh211100.orderprocessing.service;

import com.gradlehigh211100.orderprocessing.model.Order;
import com.gradlehigh211100.orderprocessing.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Implementation of the OrderService interface
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Autowired
    public OrderServiceImpl(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public Order getOrderById(Long orderId) {
        return orderRepository.findOne(orderId);
    }
}