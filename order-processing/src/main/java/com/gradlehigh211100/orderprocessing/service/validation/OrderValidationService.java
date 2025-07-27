package com.gradlehigh211100.orderprocessing.service.validation;

import com.gradlehigh211100.orderprocessing.model.entity.Order;
import com.gradlehigh211100.orderprocessing.model.entity.OrderItem;
import com.gradlehigh211100.productcatalog.service.ProductService;
import com.gradlehigh211100.userservice.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service responsible for comprehensive order validation including product availability,
 * pricing validation, customer eligibility, and business rule enforcement.
 */
@Service
public class OrderValidationService {

    private static final BigDecimal MAX_ORDER_VALUE = new BigDecimal("100000.00");
    private static final int MAX_ITEMS_PER_ORDER = 50;
    private static final int MAX_QUANTITY_PER_ITEM = 100;

    // Thresholds for different customer tiers
    private static final Map<String, Integer> CUSTOMER_TIER_ORDER_LIMITS = new HashMap<>();
    static {
        CUSTOMER_TIER_ORDER_LIMITS.put("BRONZE", 3);
        CUSTOMER_TIER_ORDER_LIMITS.put("SILVER", 5);
        CUSTOMER_TIER_ORDER_LIMITS.put("GOLD", 10);
        CUSTOMER_TIER_ORDER_LIMITS.put("PLATINUM", Integer.MAX_VALUE);
    }

    private final ProductService productService;
    private final UserService userService;
    private final Logger logger;

    /**
     * Creates a new OrderValidationService.
     *
     * @param productService service for product operations
     * @param userService service for user operations
     */
    @Autowired
    public OrderValidationService(ProductService productService, UserService userService) {
        this.productService = productService;
        this.userService = userService;
        this.logger = LoggerFactory.getLogger(OrderValidationService.class);
    }

    /**
     * Performs comprehensive order validation.
     *
     * @param order the order to validate
     * @return validation result containing validation status and any errors
     */
    public ValidationResult validateOrder(Order order) {
        logger.info("Starting comprehensive validation for order: {}", order.getId());
        
        List<String> errors = new ArrayList<>();

        // Customer validation
        if (!validateCustomer(order.getCustomerId())) {
            errors.add("Customer validation failed for ID: " + order.getCustomerId());
            logger.error("Customer validation failed for order: {}, customerId: {}", 
                    order.getId(), order.getCustomerId());
            return ValidationResult.invalid(errors);
        }

        // Product availability validation
        if (!validateProductAvailability(order)) {
            errors.add("One or more products in the order are not available in requested quantities");
            logger.error("Product availability validation failed for order: {}", order.getId());
        }

        // Pricing validation
        if (!validatePricing(order)) {
            errors.add("Pricing validation failed - current prices do not match order prices");
            logger.error("Pricing validation failed for order: {}", order.getId());
        }

        // Business rules validation
        if (!validateBusinessRules(order)) {
            errors.add("Order violates one or more business rules");
            logger.error("Business rules validation failed for order: {}", order.getId());
        }

        // Order limits validation
        if (!validateOrderLimits(order)) {
            errors.add("Order exceeds allowable limits (quantity or total value)");
            logger.error("Order limits validation failed for order: {}", order.getId());
        }

        // Additional complex validation logic
        if (isWeekendOrder(order) && isHighValueOrder(order) && !isPreferredCustomer(order.getCustomerId())) {
            errors.add("High-value weekend orders require additional approval for non-preferred customers");
            logger.warn("High-value weekend order from non-preferred customer: {}", order.getCustomerId());
        }

        // Check for possible fraud patterns
        if (hasPossibleFraudPatterns(order)) {
            errors.add("Order flagged for possible fraud review");
            logger.warn("Possible fraud patterns detected in order: {}", order.getId());
        }

        // Final result
        if (errors.isEmpty()) {
            logger.info("Order {} passed all validations successfully", order.getId());
            return ValidationResult.valid();
        } else {
            logger.info("Order {} failed validation with {} errors", order.getId(), errors.size());
            return ValidationResult.invalid(errors);
        }
    }

    /**
     * Validates customer eligibility.
     *
     * @param customerId the customer ID to validate
     * @return true if customer is eligible to place orders, false otherwise
     */
    public boolean validateCustomer(Long customerId) {
        logger.debug("Validating customer with ID: {}", customerId);
        
        if (customerId == null) {
            logger.error("Customer ID is null");
            return false;
        }

        try {
            // Check if customer exists
            if (!userService.customerExists(customerId)) {
                logger.error("Customer with ID {} does not exist", customerId);
                return false;
            }
            
            // Check if customer account is active
            if (!userService.isCustomerActive(customerId)) {
                logger.error("Customer with ID {} is not active", customerId);
                return false;
            }
            
            // Check if customer has payment methods
            if (!userService.hasValidPaymentMethods(customerId)) {
                logger.error("Customer with ID {} has no valid payment methods", customerId);
                return false;
            }
            
            // Check if customer is blocked
            if (userService.isCustomerBlocked(customerId)) {
                logger.error("Customer with ID {} is blocked", customerId);
                return false;
            }
            
            // Check outstanding orders vs tier limit
            String customerTier = userService.getCustomerTier(customerId);
            int outstandingOrders = userService.getOutstandingOrderCount(customerId);
            int tierLimit = CUSTOMER_TIER_ORDER_LIMITS.getOrDefault(customerTier, 3);
            
            if (outstandingOrders >= tierLimit) {
                logger.error("Customer with ID {} has too many outstanding orders ({}) for tier {}", 
                        customerId, outstandingOrders, customerTier);
                return false;
            }
            
            // Customer passed all validation rules
            logger.debug("Customer {} passed all validation rules", customerId);
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating customer with ID: " + customerId, e);
            return false;
        }
    }

    /**
     * Validates product availability for all order items.
     *
     * @param order the order containing items to check for availability
     * @return true if all products are available in requested quantities, false otherwise
     */
    public boolean validateProductAvailability(Order order) {
        logger.debug("Validating product availability for order: {}", order.getId());

        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            logger.error("Order is null or has no items");
            return false;
        }

        try {
            // Group products by ID to calculate total quantity per product
            Map<Long, Integer> productQuantities = new HashMap<>();
            
            for (OrderItem item : order.getItems()) {
                Long productId = item.getProductId();
                int quantity = item.getQuantity();
                
                productQuantities.put(productId, 
                        productQuantities.getOrDefault(productId, 0) + quantity);
            }
            
            // Check availability for each product
            for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
                Long productId = entry.getKey();
                Integer requestedQuantity = entry.getValue();
                
                // First check if product exists
                if (!productService.productExists(productId)) {
                    logger.error("Product with ID {} does not exist", productId);
                    return false;
                }
                
                // Then check if product is active/available
                if (!productService.isProductActive(productId)) {
                    logger.error("Product with ID {} is not active", productId);
                    return false;
                }
                
                // Check inventory levels
                int availableStock = productService.getAvailableStock(productId);
                if (availableStock < requestedQuantity) {
                    logger.error("Insufficient stock for product {}: requested {}, available {}", 
                            productId, requestedQuantity, availableStock);
                    return false;
                }
                
                // Check if product has any restrictions that might prevent ordering
                if (productService.hasOrderingRestrictions(productId)) {
                    logger.error("Product {} has ordering restrictions", productId);
                    return false;
                }
            }
            
            // All products available in requested quantities
            logger.debug("All products are available for order {}", order.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating product availability for order: " + order.getId(), e);
            return false;
        }
    }

    /**
     * Validates pricing information against current prices.
     *
     * @param order the order to validate pricing for
     * @return true if pricing information is valid, false otherwise
     */
    public boolean validatePricing(Order order) {
        logger.debug("Validating pricing for order: {}", order.getId());

        if (order == null || order.getItems() == null || order.getItems().isEmpty()) {
            logger.error("Order is null or has no items");
            return false;
        }

        try {
            boolean pricingValid = true;
            BigDecimal calculatedTotal = BigDecimal.ZERO;
            
            for (OrderItem item : order.getItems()) {
                Long productId = item.getProductId();
                BigDecimal currentPrice = productService.getCurrentPrice(productId);
                BigDecimal orderPrice = item.getUnitPrice();
                
                // Allow for small price differences (e.g., due to rounding)
                BigDecimal priceDifference = currentPrice.subtract(orderPrice).abs();
                BigDecimal allowableDifference = currentPrice.multiply(new BigDecimal("0.01")); // 1% tolerance
                
                // Check if price difference exceeds tolerance
                if (priceDifference.compareTo(allowableDifference) > 0) {
                    logger.error("Price mismatch for product {}: order price {}, current price {}", 
                            productId, orderPrice, currentPrice);
                    pricingValid = false;
                }
                
                // Calculate expected total
                BigDecimal itemTotal = currentPrice.multiply(new BigDecimal(item.getQuantity()));
                calculatedTotal = calculatedTotal.add(itemTotal);
            }
            
            // Verify total price with calculated price (considering discounts)
            BigDecimal orderTotal = order.getTotalAmount();
            BigDecimal discountAmount = order.getDiscountAmount() != null ? 
                    order.getDiscountAmount() : BigDecimal.ZERO;
            BigDecimal expectedTotal = calculatedTotal.subtract(discountAmount);
            
            // Allow for small total differences (e.g., due to rounding)
            BigDecimal totalDifference = expectedTotal.subtract(orderTotal).abs();
            BigDecimal allowableTotalDifference = expectedTotal.multiply(new BigDecimal("0.01")); // 1% tolerance
            
            if (totalDifference.compareTo(allowableTotalDifference) > 0) {
                logger.error("Order total mismatch: order total {}, calculated total {}", 
                        orderTotal, expectedTotal);
                pricingValid = false;
            }
            
            // Apply special discount validations if needed
            if (order.getDiscountAmount() != null && order.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                if (!validateDiscounts(order)) {
                    logger.error("Invalid discount applied to order {}", order.getId());
                    pricingValid = false;
                }
            }
            
            return pricingValid;
            
        } catch (Exception e) {
            logger.error("Error validating pricing for order: " + order.getId(), e);
            return false;
        }
    }

    /**
     * Validates order against business rules.
     *
     * @param order the order to validate
     * @return true if order complies with all business rules, false otherwise
     */
    public boolean validateBusinessRules(Order order) {
        logger.debug("Validating business rules for order: {}", order.getId());

        if (order == null) {
            logger.error("Order is null");
            return false;
        }

        try {
            // Rule 1: Check for restricted products based on customer location
            if (!validateLocationBasedRestrictions(order)) {
                logger.error("Location-based restrictions violated for order {}", order.getId());
                return false;
            }
            
            // Rule 2: Check for product combinations that are not allowed
            if (!validateProductCombinations(order)) {
                logger.error("Invalid product combination in order {}", order.getId());
                return false;
            }
            
            // Rule 3: Check for time-based restrictions (e.g., limited time offers)
            if (!validateTimeBasedRestrictions(order)) {
                logger.error("Time-based restrictions violated for order {}", order.getId());
                return false;
            }
            
            // Rule 4: Check for customer-specific purchase limits
            if (!validateCustomerPurchaseLimits(order)) {
                logger.error("Customer purchase limits exceeded in order {}", order.getId());
                return false;
            }
            
            // Rule 5: Check for required complementary products
            if (!validateRequiredComplementaryProducts(order)) {
                logger.error("Missing required complementary products in order {}", order.getId());
                return false;
            }
            
            // Rule 6: Check for proper authorization for restricted products
            if (!validateRestrictedProductAuthorization(order)) {
                logger.error("Unauthorized restricted products in order {}", order.getId());
                return false;
            }
            
            // Rule 7: Check for valid shipping destination
            if (!validateShippingDestination(order)) {
                logger.error("Invalid shipping destination for order {}", order.getId());
                return false;
            }
            
            // Rule 8: Check for minimum order requirements
            if (!validateMinimumOrderRequirements(order)) {
                logger.error("Minimum order requirements not met for order {}", order.getId());
                return false;
            }
            
            // All business rules passed
            logger.debug("Order {} passed all business rule validations", order.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating business rules for order: " + order.getId(), e);
            return false;
        }
    }

    /**
     * Validates order against quantity and value limits.
     *
     * @param order the order to validate
     * @return true if order is within limits, false otherwise
     */
    public boolean validateOrderLimits(Order order) {
        logger.debug("Validating order limits for order: {}", order.getId());

        if (order == null || order.getItems() == null) {
            logger.error("Order is null or has no items");
            return false;
        }

        try {
            // Check total order value against maximum allowed
            if (order.getTotalAmount().compareTo(MAX_ORDER_VALUE) > 0) {
                logger.error("Order {} exceeds maximum allowed value: {} > {}", 
                        order.getId(), order.getTotalAmount(), MAX_ORDER_VALUE);
                return false;
            }
            
            // Check total number of items against maximum allowed
            if (order.getItems().size() > MAX_ITEMS_PER_ORDER) {
                logger.error("Order {} exceeds maximum allowed items: {} > {}", 
                        order.getId(), order.getItems().size(), MAX_ITEMS_PER_ORDER);
                return false;
            }
            
            // Check quantity per product
            for (OrderItem item : order.getItems()) {
                if (item.getQuantity() > MAX_QUANTITY_PER_ITEM) {
                    logger.error("Order {} has item with excessive quantity: product {} has quantity {} > {}", 
                            order.getId(), item.getProductId(), item.getQuantity(), MAX_QUANTITY_PER_ITEM);
                    return false;
                }
                
                // Additional validation for specific product categories
                if (isRestrictedCategory(item.getProductId())) {
                    int categoryLimit = getCategoryQuantityLimit(item.getProductId());
                    if (item.getQuantity() > categoryLimit) {
                        logger.error("Order {} exceeds limit for restricted category product {}: {} > {}", 
                                order.getId(), item.getProductId(), item.getQuantity(), categoryLimit);
                        return false;
                    }
                }
            }
            
            // Customer-specific order value limits
            BigDecimal customerValueLimit = getCustomerOrderValueLimit(order.getCustomerId());
            if (order.getTotalAmount().compareTo(customerValueLimit) > 0) {
                logger.error("Order {} exceeds customer-specific value limit: {} > {}", 
                        order.getId(), order.getTotalAmount(), customerValueLimit);
                return false;
            }
            
            // Check for excessive quantities of the same product
            Map<Long, Integer> productQuantities = new HashMap<>();
            for (OrderItem item : order.getItems()) {
                productQuantities.put(item.getProductId(), 
                        productQuantities.getOrDefault(item.getProductId(), 0) + item.getQuantity());
            }
            
            for (Map.Entry<Long, Integer> entry : productQuantities.entrySet()) {
                if (entry.getValue() > MAX_QUANTITY_PER_ITEM) {
                    logger.error("Order {} has excessive quantity for product {}: {} > {}", 
                            order.getId(), entry.getKey(), entry.getValue(), MAX_QUANTITY_PER_ITEM);
                    return false;
                }
            }
            
            logger.debug("Order {} is within all quantity and value limits", order.getId());
            return true;
            
        } catch (Exception e) {
            logger.error("Error validating order limits for order: " + order.getId(), e);
            return false;
        }
    }

    //
    // Private helper methods
    //
    
    private boolean validateDiscounts(Order order) {
        // Complex discount validation logic
        try {
            BigDecimal orderTotal = order.getTotalAmount().add(order.getDiscountAmount());
            BigDecimal maxAllowedDiscount = orderTotal.multiply(new BigDecimal("0.30")); // Max 30% discount
            
            if (order.getDiscountAmount().compareTo(maxAllowedDiscount) > 0) {
                logger.error("Discount amount {} exceeds maximum allowed discount {}", 
                        order.getDiscountAmount(), maxAllowedDiscount);
                return false;
            }
            
            // FIXME: Add validation for discount codes when they are implemented
            
            return true;
        } catch (Exception e) {
            logger.error("Error validating discounts", e);
            return false;
        }
    }
    
    private boolean validateLocationBasedRestrictions(Order order) {
        // Mock implementation - would connect to location validation service
        return true; // Simplified for this example
    }
    
    private boolean validateProductCombinations(Order order) {
        // Check for incompatible product combinations
        List<Long> productIds = order.getItems().stream()
                .map(OrderItem::getProductId)
                .collect(Collectors.toList());
        
        // TODO: Implement actual product combination validation logic
        return true; // Simplified for this example
    }
    
    private boolean validateTimeBasedRestrictions(Order order) {
        // Check for time-based restrictions like limited offers
        // TODO: Implement actual time-based validation logic
        return true; // Simplified for this example
    }
    
    private boolean validateCustomerPurchaseLimits(Order order) {
        // Check if customer has exceeded purchase limits
        // TODO: Connect to customer purchase history service
        return true; // Simplified for this example
    }
    
    private boolean validateRequiredComplementaryProducts(Order order) {
        // Check for required complementary products
        // TODO: Implement actual complementary product validation logic
        return true; // Simplified for this example
    }
    
    private boolean validateRestrictedProductAuthorization(Order order) {
        // Check for authorization for restricted products
        // TODO: Implement actual restricted product validation logic
        return true; // Simplified for this example
    }
    
    private boolean validateShippingDestination(Order order) {
        // Check if shipping destination is valid
        // TODO: Connect to shipping service for validation
        return true; // Simplified for this example
    }
    
    private boolean validateMinimumOrderRequirements(Order order) {
        // Check if order meets minimum requirements
        return order.getTotalAmount().compareTo(BigDecimal.ZERO) > 0;
    }
    
    private boolean isRestrictedCategory(Long productId) {
        // Check if product belongs to a restricted category
        // TODO: Implement actual category restriction checking
        return false; // Simplified for this example
    }
    
    private int getCategoryQuantityLimit(Long productId) {
        // Get quantity limit for product category
        // TODO: Implement actual category limit lookup
        return 10; // Default limit
    }
    
    private BigDecimal getCustomerOrderValueLimit(Long customerId) {
        // Get value limit for customer
        String customerTier = userService.getCustomerTier(customerId);
        
        // Default tier limits
        Map<String, BigDecimal> tierLimits = new HashMap<>();
        tierLimits.put("BRONZE", new BigDecimal("5000"));
        tierLimits.put("SILVER", new BigDecimal("10000"));
        tierLimits.put("GOLD", new BigDecimal("25000"));
        tierLimits.put("PLATINUM", new BigDecimal("100000"));
        
        return tierLimits.getOrDefault(customerTier, new BigDecimal("5000"));
    }
    
    private boolean isWeekendOrder(Order order) {
        // Check if order was placed on weekend
        // TODO: Implement actual weekend check
        return false; // Simplified for this example
    }
    
    private boolean isHighValueOrder(Order order) {
        // Check if order value is considered "high"
        BigDecimal highValueThreshold = new BigDecimal("10000");
        return order.getTotalAmount().compareTo(highValueThreshold) >= 0;
    }
    
    private boolean isPreferredCustomer(Long customerId) {
        // Check if customer is in preferred category
        String tier = userService.getCustomerTier(customerId);
        return "GOLD".equals(tier) || "PLATINUM".equals(tier);
    }
    
    private boolean hasPossibleFraudPatterns(Order order) {
        // Check for possible fraud patterns
        
        // Pattern 1: Unusually large quantities of expensive items
        boolean hasLargeExpensiveOrder = order.getItems().stream()
                .anyMatch(item -> item.getQuantity() > 10 && 
                        item.getUnitPrice().compareTo(new BigDecimal("1000")) > 0);
        
        // Pattern 2: Multiple different shipping and billing addresses recently
        // TODO: Implement connection to fraud detection service
        
        // Pattern 3: Orders from unusual locations
        // TODO: Implement geolocation validation
        
        // For this example, just check pattern 1
        return hasLargeExpensiveOrder;
    }
}