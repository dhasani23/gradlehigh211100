package com.gradlehigh211100.orderprocessing.repository;

import com.gradlehigh211100.orderprocessing.model.OrderHistory;
import com.gradlehigh211100.orderprocessing.model.enums.OrderEvent;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository interface for OrderHistory entity operations.
 */
@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {
    
    /**
     * Find history entries for a specific order.
     */
    List<OrderHistory> findByOrderIdOrderByTransitionTimestampDesc(Long orderId);
    
    /**
     * Find history entries by previous state.
     */
    List<OrderHistory> findByPreviousState(OrderState previousState);
    
    /**
     * Find history entries by current state.
     */
    List<OrderHistory> findByCurrentState(OrderState currentState);
    
    /**
     * Find history entries by event.
     */
    List<OrderHistory> findByEvent(OrderEvent event);
    
    /**
     * Find history entries by time period.
     */
    List<OrderHistory> findByTransitionTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Find the most recent state transition for an order.
     */
    OrderHistory findTopByOrderIdOrderByTransitionTimestampDesc(Long orderId);
    
    /**
     * Custom query to find average time spent in each state.
     */
    @Query("SELECT h.previousState, AVG(TIMESTAMPDIFF(MINUTE, h1.transitionTimestamp, h.transitionTimestamp)) " +
           "FROM OrderHistory h " +
           "JOIN OrderHistory h1 ON h.orderId = h1.orderId AND h1.currentState = h.previousState " +
           "GROUP BY h.previousState")
    List<Object[]> findAverageTimeInEachState();
    
    /**
     * Custom query to find the most common transition paths.
     */
    @Query("SELECT h.previousState, h.currentState, COUNT(*) " +
           "FROM OrderHistory h " +
           "GROUP BY h.previousState, h.currentState " +
           "ORDER BY COUNT(*) DESC")
    List<Object[]> findMostCommonTransitions();
}