package com.gradlehigh211100.orderprocessing.repository;

import com.gradlehigh211100.orderprocessing.model.entity.Order;
import com.gradlehigh211100.orderprocessing.model.enums.OrderState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface providing data access methods for Order entity
 * including complex queries for order searching, filtering, and reporting.
 * 
 * This repository implements high cyclomatic complexity business rules
 * for order management and search capabilities.
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * Finds all orders for a specific customer
     *
     * @param customerId the customer ID
     * @return list of orders belonging to the customer
     */
    List<Order> findByCustomerId(Long customerId);
    
    /**
     * Finds order by its unique order number
     *
     * @param orderNumber the order number
     * @return an Optional containing the order if found, empty otherwise
     */
    Optional<Order> findByOrderNumber(String orderNumber);
    
    /**
     * Finds all orders in a specific state
     *
     * @param orderState the state of orders to find
     * @return list of orders in the specified state
     */
    List<Order> findByOrderState(OrderState orderState);
    
    /**
     * Finds orders for a specific customer that are in a specific state
     *
     * @param customerId the customer ID
     * @param orderState the state of orders to find
     * @return list of orders matching both criteria
     */
    List<Order> findByCustomerIdAndOrderState(Long customerId, OrderState orderState);
    
    /**
     * Finds orders created within a specific date range
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @return list of orders within the date range
     */
    List<Order> findByOrderDateBetween(Date startDate, Date endDate);
    
    /**
     * Counts the number of orders in a specific state
     *
     * @param orderState the state of orders to count
     * @return count of orders in the specified state
     */
    Long countByOrderState(OrderState orderState);
    
    /**
     * Finds orders with total amount greater than the specified value
     * 
     * @param amount the minimum total amount
     * @return list of orders with total amount greater than specified
     */
    @Query("SELECT o FROM Order o WHERE o.totalAmount > :amount")
    List<Order> findOrdersWithTotalGreaterThan(@Param("amount") Double amount);
    
    /**
     * Finds the top spending customers within a date range
     * 
     * @param startDate the start date
     * @param endDate the end date
     * @param limit the number of top customers to return
     * @return list of customer IDs with their total spending
     */
    @Query(value = 
        "SELECT o.customer_id, SUM(o.total_amount) AS total_spent " +
        "FROM orders o " +
        "WHERE o.order_date BETWEEN :startDate AND :endDate " +
        "GROUP BY o.customer_id " +
        "ORDER BY total_spent DESC " +
        "LIMIT :limit", 
        nativeSQL = true)
    List<Object[]> findTopSpendingCustomers(
        @Param("startDate") Date startDate, 
        @Param("endDate") Date endDate,
        @Param("limit") int limit);
    
    /**
     * Find orders that have items from specific product categories
     *
     * @param categories list of product category IDs
     * @return list of orders containing products from the specified categories
     */
    @Query(
        "SELECT DISTINCT o FROM Order o JOIN o.orderItems oi " +
        "JOIN oi.product p JOIN p.category c " +
        "WHERE c.id IN :categories"
    )
    List<Order> findOrdersWithProductCategories(@Param("categories") List<Long> categories);
    
    /**
     * Find orders with specific shipping methods and states
     * 
     * @param shippingMethod the shipping method code
     * @param states list of order states to include
     * @return list of matching orders
     */
    @Query(
        "SELECT o FROM Order o " +
        "WHERE o.shippingMethod = :shippingMethod " +
        "AND o.orderState IN :states"
    )
    List<Order> findOrdersByShippingMethodAndStates(
        @Param("shippingMethod") String shippingMethod,
        @Param("states") List<OrderState> states);
    
    /**
     * Find orders that might be fraudulent based on complex criteria
     * 
     * @param thresholdAmount the minimum amount that triggers review
     * @param suspiciousIPs list of suspicious IP addresses
     * @return list of potentially fraudulent orders
     */
    @Query(
        "SELECT o FROM Order o " +
        "WHERE (o.totalAmount > :thresholdAmount AND o.ipAddress IN :suspiciousIPs) " +
        "OR (o.billingAddress.country != o.shippingAddress.country) " +
        "OR (o.paymentAttempts > 3)"
    )
    List<Order> findPotentialFraudulentOrders(
        @Param("thresholdAmount") Double thresholdAmount,
        @Param("suspiciousIPs") List<String> suspiciousIPs);
    
    /**
     * Calculate average order processing time by day of week
     * 
     * FIXME: This query needs optimization for large datasets
     * 
     * @return map of day of week to average processing time in minutes
     */
    @Query(value = 
        "SELECT " +
        "  EXTRACT(DOW FROM o.order_date) AS day_of_week, " +
        "  AVG(EXTRACT(EPOCH FROM (o.completed_date - o.order_date))/60) AS avg_minutes " +
        "FROM orders o " +
        "WHERE o.order_state = 'COMPLETED' " +
        "GROUP BY day_of_week " +
        "ORDER BY day_of_week",
        nativeSQL = true)
    List<Object[]> calculateAverageProcessingTimeByDayOfWeek();
    
    /**
     * Find orders that have been in the current state for too long and require attention
     * 
     * TODO: Implement timeout configuration per state
     * 
     * @param stateTimeoutHours map of order states to their timeout in hours
     * @return list of orders that have exceeded their state timeout
     */
    @Query(
        "SELECT o FROM Order o " +
        "WHERE " +
        "  (o.orderState = 'PENDING' AND o.lastStateChange < :pendingThreshold) OR " +
        "  (o.orderState = 'PROCESSING' AND o.lastStateChange < :processingThreshold) OR " +
        "  (o.orderState = 'SHIPPING' AND o.lastStateChange < :shippingThreshold)"
    )
    List<Order> findOrdersExceedingStateTimeout(
        @Param("pendingThreshold") Date pendingThreshold,
        @Param("processingThreshold") Date processingThreshold,
        @Param("shippingThreshold") Date shippingThreshold);
}