package com.gradlehigh211100.orderprocessing.model.reservation;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents the result of an inventory reservation operation.
 * Contains information about success/failure and details about any products
 * that couldn't be reserved due to stock limitations.
 */
public class ReservationResult {
    private boolean successful;
    private Map<Long, Integer> unavailableProducts;
    private String message;
    
    /**
     * Default constructor for unsuccessful reservation
     */
    public ReservationResult() {
        this.successful = false;
        this.unavailableProducts = new HashMap<>();
        this.message = "Reservation failed";
    }
    
    /**
     * Constructor for creating a successful reservation result
     * 
     * @param successful true if reservation was successful
     */
    public ReservationResult(boolean successful) {
        this.successful = successful;
        this.unavailableProducts = new HashMap<>();
        this.message = successful ? "Reservation successful" : "Reservation failed";
    }

    /**
     * Add a product that couldn't be reserved due to insufficient inventory
     * 
     * @param productId the ID of the unavailable product
     * @param requestedQuantity the requested quantity that couldn't be fulfilled
     */
    public void addUnavailableProduct(Long productId, Integer requestedQuantity) {
        unavailableProducts.put(productId, requestedQuantity);
    }
    
    /**
     * @return true if reservation was successful, false otherwise
     */
    public boolean isSuccessful() {
        return successful;
    }

    /**
     * @param successful set the success status of this reservation
     */
    public void setSuccessful(boolean successful) {
        this.successful = successful;
    }

    /**
     * @return map of unavailable products with their requested quantities
     */
    public Map<Long, Integer> getUnavailableProducts() {
        return unavailableProducts;
    }

    /**
     * @param unavailableProducts set map of unavailable products
     */
    public void setUnavailableProducts(Map<Long, Integer> unavailableProducts) {
        this.unavailableProducts = unavailableProducts;
    }

    /**
     * @return the reservation result message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message set the reservation result message
     */
    public void setMessage(String message) {
        this.message = message;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("ReservationResult{successful=").append(successful)
          .append(", message='").append(message).append("'");
        
        if (!unavailableProducts.isEmpty()) {
            sb.append(", unavailableProducts=").append(unavailableProducts);
        }
        
        sb.append("}");
        return sb.toString();
    }
}