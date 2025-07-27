package com.gradlehigh211100.orderprocessing.repository;

import com.gradlehigh211100.orderprocessing.model.Order;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for Order entity operations.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    
    /**
     * Find orders by their current state.
     */
    List<Order> findByState(OrderState state);
    
    /**
     * Find orders by customer email.
     */
    List<Order> findByCustomerEmail(String email);
    
    /**
     * Find orders created between two dates.
     */
    List<Order> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find orders in a specific state created before a certain date.
     */
    List<Order> findByStateAndCreatedAtBefore(OrderState state, LocalDateTime date);
}