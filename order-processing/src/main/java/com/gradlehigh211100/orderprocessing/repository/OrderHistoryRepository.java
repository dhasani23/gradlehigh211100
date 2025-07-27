package com.gradlehigh211100.orderprocessing.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.gradlehigh211100.orderprocessing.model.entity.OrderHistory;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;

/**
 * Repository interface for order history tracking providing methods to query 
 * historical order state changes and audit trails.
 * 
 * This repository provides functionality to:
 * - Track and query the state change history of orders
 * - Retrieve audit trails for compliance and debugging
 * - Filter history entries by different criteria (date ranges, states, etc.)
 */
@Repository
public interface OrderHistoryRepository extends JpaRepository<OrderHistory, Long> {

    /**
     * Finds all history entries for a specific order.
     *
     * @param orderId the ID of the order to find history for
     * @return a list of order history entries for the specified order
     */
    List<OrderHistory> findByOrderId(Long orderId);
    
    /**
     * Finds order history entries sorted by date in descending order.
     * This is useful for viewing the most recent state changes first.
     *
     * @param orderId the ID of the order to find history for
     * @return a chronologically sorted (newest first) list of history entries
     */
    List<OrderHistory> findByOrderIdOrderByStateChangeDateDesc(Long orderId);
    
    /**
     * Finds all history entries for a specific state.
     * This allows tracking how many orders transitioned to a particular state.
     *
     * @param newState the state to search for
     * @return a list of history entries where orders transitioned to the specified state
     */
    List<OrderHistory> findByNewState(OrderState newState);
    
    /**
     * Finds history entries within a specific date range.
     * Useful for auditing and reporting on state changes during a time period.
     *
     * @param startDate the beginning of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @return a list of history entries occurring within the specified date range
     */
    List<OrderHistory> findByStateChangeDateBetween(Date startDate, Date endDate);
    
    /**
     * Finds history entries for orders that transitioned from one specific state to another.
     * 
     * @param oldState the state the order transitioned from
     * @param newState the state the order transitioned to
     * @return a list of history entries matching the state transition criteria
     */
    List<OrderHistory> findByOldStateAndNewState(OrderState oldState, OrderState newState);
    
    /**
     * Finds history entries by the user who made the state change.
     * Useful for auditing user actions.
     *
     * @param username the username of the person who made the state change
     * @return a list of history entries initiated by the specified user
     */
    List<OrderHistory> findByChangedBy(String username);
    
    /**
     * Counts the number of state changes for a specific order.
     * Useful for identifying orders with complex processing histories.
     *
     * @param orderId the ID of the order
     * @return the count of state changes
     */
    Long countByOrderId(Long orderId);
    
    /**
     * Finds recent history entries for a specific order with limit.
     * 
     * @param orderId the ID of the order
     * @param limit maximum number of entries to return
     * @return limited list of most recent history entries
     */
    @Query(value = "SELECT oh FROM OrderHistory oh WHERE oh.orderId = :orderId " +
           "ORDER BY oh.stateChangeDate DESC LIMIT :limit")
    List<OrderHistory> findRecentHistoryByOrderId(@Param("orderId") Long orderId, @Param("limit") int limit);
    
    /**
     * Finds all orders that transitioned to a specific state within a time range.
     * Useful for analyzing how quickly orders move through the workflow.
     *
     * @param state the target state
     * @param startDate beginning of date range
     * @param endDate end of date range
     * @return list of matching history entries
     */
    @Query("SELECT oh FROM OrderHistory oh WHERE oh.newState = :state " +
           "AND oh.stateChangeDate BETWEEN :startDate AND :endDate " +
           "ORDER BY oh.stateChangeDate ASC")
    List<OrderHistory> findOrdersTransitionedToStateInTimeRange(
            @Param("state") OrderState state, 
            @Param("startDate") Date startDate, 
            @Param("endDate") Date endDate);
    
    /**
     * Calculates average time spent in each state for a specific order.
     * 
     * FIXME: This query needs optimization for large datasets
     * TODO: Consider moving this complex calculation to a separate service
     * 
     * @param orderId the ID of the order
     * @return complex object containing state transition time analytics
     */
    @Query(value = "WITH state_durations AS (" +
           "  SELECT oh1.new_state as state, " +
           "         MIN(oh2.state_change_date) - oh1.state_change_date as duration " +
           "  FROM order_history oh1 " +
           "  LEFT JOIN order_history oh2 ON oh1.order_id = oh2.order_id " +
           "     AND oh2.state_change_date > oh1.state_change_date " +
           "  WHERE oh1.order_id = :orderId " +
           "  GROUP BY oh1.id, oh1.new_state, oh1.state_change_date" +
           ") " +
           "SELECT state, AVG(duration) as avg_duration " +
           "FROM state_durations " +
           "GROUP BY state", nativeQuery = true)
    List<Object[]> calculateAverageTimeInStates(@Param("orderId") Long orderId);
}