package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Random;
import java.util.logging.Logger;

/**
 * Entity containing shipping information including delivery address, shipping method,
 * tracking number, and delivery status.
 * This class handles all aspects of order shipping and delivery tracking.
 */
public class ShippingDetails extends BaseEntity {
    
    private static final Logger LOGGER = Logger.getLogger(ShippingDetails.class.getName());
    private static final BigDecimal BASE_RATE = new BigDecimal("5.99");
    private static final BigDecimal WEIGHT_FACTOR = new BigDecimal("2.5");
    private static final BigDecimal DISTANCE_FACTOR = new BigDecimal("0.15");
    private static final BigDecimal EXPEDITED_RATE = new BigDecimal("1.75");
    private static final String[] SHIPPING_PROVIDERS = {
        "FedEx", "UPS", "DHL", "USPS", "Royal Mail", "Canada Post"
    };
    
    // Shipping address components
    private String shippingAddress;  // Complete shipping address
    private String city;             // Shipping city
    private String state;            // Shipping state/province
    private String zipCode;          // Shipping postal code
    private String country;          // Shipping country
    
    // Shipping method and cost
    private String shippingMethod;   // Selected shipping method (Standard, Express, Priority)
    private BigDecimal shippingCost; // Cost of shipping
    
    // Tracking information
    private String trackingNumber;   // Tracking number for shipment
    private String carrierName;      // Name of shipping carrier
    
    // Delivery dates
    private Date estimatedDeliveryDate;  // Estimated delivery date
    private Date actualDeliveryDate;     // Actual delivery date
    
    // Internal tracking fields
    private boolean isDelivered;
    private int deliveryAttempts;
    private String deliveryNotes;
    private boolean requiresSignature;
    private boolean isInsured;
    private BigDecimal insuranceAmount;
    private boolean isPriority;
    
    /**
     * Default constructor
     */
    public ShippingDetails() {
        this.deliveryAttempts = 0;
        this.isDelivered = false;
        this.requiresSignature = false;
        this.isInsured = false;
        this.insuranceAmount = BigDecimal.ZERO;
        this.isPriority = false;
    }
    
    /**
     * Constructor with essential shipping information
     * 
     * @param shippingAddress The complete shipping address
     * @param city The shipping city
     * @param state The shipping state/province
     * @param zipCode The shipping postal code
     * @param country The shipping country
     * @param shippingMethod The selected shipping method
     */
    public ShippingDetails(String shippingAddress, String city, String state, 
                          String zipCode, String country, String shippingMethod) {
        this();
        this.shippingAddress = shippingAddress;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        this.shippingMethod = shippingMethod;
        
        // Set default values for other fields
        this.deliveryNotes = "";
    }
    
    /**
     * Calculates shipping cost based on weight, distance, and shipping method
     * Formula includes:
     * - Base rate
     * - Weight multiplier
     * - Distance factor
     * - Special handling fees
     * - Expedited shipping multiplier (if applicable)
     * 
     * @param weight The weight of the package in pounds
     * @param distance The shipping distance in miles
     * @return The calculated shipping cost
     */
    public BigDecimal calculateShippingCost(BigDecimal weight, BigDecimal distance) {
        // Log calculation start
        LOGGER.info("Calculating shipping cost for weight: " + weight + " lbs, distance: " + distance + " miles");
        
        // Validate inputs to prevent invalid calculations
        if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.warning("Invalid weight provided: " + weight);
            throw new IllegalArgumentException("Weight must be greater than zero");
        }
        
        if (distance == null || distance.compareTo(BigDecimal.ZERO) <= 0) {
            LOGGER.warning("Invalid distance provided: " + distance);
            throw new IllegalArgumentException("Distance must be greater than zero");
        }
        
        // Complex calculation with multiple factors
        BigDecimal cost = BASE_RATE;
        
        // Apply weight factor with progressive scaling
        if (weight.compareTo(new BigDecimal("10")) > 0) {
            BigDecimal overWeight = weight.subtract(new BigDecimal("10"));
            cost = cost.add(WEIGHT_FACTOR.multiply(weight))
                      .add(overWeight.multiply(new BigDecimal("0.75")));
        } else {
            cost = cost.add(WEIGHT_FACTOR.multiply(weight));
        }
        
        // Apply distance factor with tiered pricing
        if (distance.compareTo(new BigDecimal("500")) <= 0) {
            cost = cost.add(DISTANCE_FACTOR.multiply(distance));
        } else if (distance.compareTo(new BigDecimal("1000")) <= 0) {
            // First 500 miles at standard rate
            BigDecimal standardDistance = new BigDecimal("500");
            // Remaining at discounted rate
            BigDecimal discountedDistance = distance.subtract(standardDistance);
            cost = cost.add(DISTANCE_FACTOR.multiply(standardDistance))
                      .add(DISTANCE_FACTOR.multiply(discountedDistance).multiply(new BigDecimal("0.9")));
        } else {
            // Complex multi-tiered calculation for long distances
            BigDecimal tier1Distance = new BigDecimal("500");
            BigDecimal tier2Distance = new BigDecimal("500"); // 500 to 1000
            BigDecimal tier3Distance = distance.subtract(new BigDecimal("1000")); // over 1000
            
            cost = cost.add(DISTANCE_FACTOR.multiply(tier1Distance))
                      .add(DISTANCE_FACTOR.multiply(tier2Distance).multiply(new BigDecimal("0.9")))
                      .add(DISTANCE_FACTOR.multiply(tier3Distance).multiply(new BigDecimal("0.75")));
        }
        
        // Apply shipping method multiplier
        if ("Express".equalsIgnoreCase(shippingMethod)) {
            cost = cost.multiply(EXPEDITED_RATE);
        } else if ("Priority".equalsIgnoreCase(shippingMethod)) {
            cost = cost.multiply(EXPEDITED_RATE).multiply(new BigDecimal("1.5"));
            isPriority = true;
        } else if ("Economy".equalsIgnoreCase(shippingMethod)) {
            cost = cost.multiply(new BigDecimal("0.8"));
        }
        
        // International shipping surcharge
        if (!"United States".equalsIgnoreCase(country)) {
            // Apply different rates based on country regions
            if (isEuropeanCountry(country)) {
                cost = cost.multiply(new BigDecimal("1.35"));
            } else if (isAsianCountry(country)) {
                cost = cost.multiply(new BigDecimal("1.5"));
            } else {
                cost = cost.multiply(new BigDecimal("1.75"));
            }
        }
        
        // Apply insurance if needed (for expensive shipments)
        if (cost.compareTo(new BigDecimal("50")) > 0) {
            isInsured = true;
            insuranceAmount = cost.multiply(new BigDecimal("0.01"));
            cost = cost.add(insuranceAmount);
        }
        
        // Handle special cases and edge conditions
        if (requiresSignature) {
            cost = cost.add(new BigDecimal("5.25"));
        }
        
        // FIXME: Rounding errors can accumulate in the calculation
        // Round to 2 decimal places
        cost = cost.setScale(2, BigDecimal.ROUND_HALF_UP);
        
        // Cache the result
        this.shippingCost = cost;
        
        LOGGER.info("Calculated shipping cost: " + cost);
        return cost;
    }
    
    /**
     * Updates tracking information for the shipment
     * 
     * @param trackingNumber The tracking number for the shipment
     * @param carrier The carrier handling the shipment
     */
    public void updateTrackingInfo(String trackingNumber, String carrier) {
        // Validate tracking number format based on carrier
        if (!isValidTrackingNumber(trackingNumber, carrier)) {
            LOGGER.warning("Invalid tracking number format: " + trackingNumber + " for carrier: " + carrier);
            throw new IllegalArgumentException("Invalid tracking number format for " + carrier);
        }
        
        this.trackingNumber = trackingNumber;
        this.carrierName = carrier;
        
        // Update estimated delivery date based on shipping method and carrier
        calculateEstimatedDeliveryDate();
        
        LOGGER.info("Tracking information updated: " + carrier + " - " + trackingNumber);
    }
    
    /**
     * Marks the shipment as delivered
     * 
     * @param deliveryDate The actual delivery date
     */
    public void markAsDelivered(Date deliveryDate) {
        // Validate delivery date
        Date currentDate = new Date();
        if (deliveryDate != null && deliveryDate.after(currentDate)) {
            LOGGER.warning("Invalid delivery date in the future: " + deliveryDate);
            throw new IllegalArgumentException("Delivery date cannot be in the future");
        }
        
        // Check if estimated delivery date exists
        if (estimatedDeliveryDate == null) {
            LOGGER.warning("Marking as delivered but no estimated delivery date was set");
        }
        
        // Set delivery details
        this.isDelivered = true;
        this.actualDeliveryDate = deliveryDate != null ? deliveryDate : currentDate;
        
        // Calculate if delivery was on time
        boolean isOnTime = isDeliveredOnTime();
        
        // Log delivery status
        if (isOnTime) {
            LOGGER.info("Package delivered on time on " + actualDeliveryDate);
        } else {
            LOGGER.info("Package delivered late on " + actualDeliveryDate + 
                      " (Expected: " + estimatedDeliveryDate + ")");
        }
        
        // Update delivery notes
        if (deliveryAttempts > 1) {
            this.deliveryNotes += "Delivered after " + deliveryAttempts + " attempts. ";
        }
        
        this.deliveryNotes += "Delivered on " + actualDeliveryDate + ". ";
    }
    
    /**
     * Records a failed delivery attempt
     * 
     * @param reason The reason for the failed delivery attempt
     * @return The updated number of delivery attempts
     */
    public int recordFailedDeliveryAttempt(String reason) {
        this.deliveryAttempts++;
        
        // Update delivery notes
        this.deliveryNotes += "Delivery attempt " + deliveryAttempts + " failed: " + reason + ". ";
        
        // Recalculate estimated delivery date
        Date newEstimate = new Date(this.estimatedDeliveryDate.getTime() + (24 * 60 * 60 * 1000));
        this.estimatedDeliveryDate = newEstimate;
        
        LOGGER.info("Failed delivery attempt recorded. Reason: " + reason + 
                  ". New estimated delivery: " + estimatedDeliveryDate);
        
        return this.deliveryAttempts;
    }
    
    /**
     * Determines if a tracking number is valid for the specified carrier
     * Different carriers have different tracking number formats
     * 
     * @param trackingNumber The tracking number to validate
     * @param carrier The carrier name
     * @return True if the tracking number is valid for the carrier
     */
    private boolean isValidTrackingNumber(String trackingNumber, String carrier) {
        if (trackingNumber == null || trackingNumber.trim().isEmpty()) {
            return false;
        }
        
        // TODO: Implement proper regex validation for each carrier format
        // Currently just doing basic validation
        switch (carrier) {
            case "FedEx":
                return trackingNumber.length() == 12 || trackingNumber.length() == 15;
            case "UPS":
                return trackingNumber.length() == 18;
            case "DHL":
                return trackingNumber.length() == 10;
            case "USPS":
                return trackingNumber.length() == 22;
            default:
                return trackingNumber.length() > 5;
        }
    }
    
    /**
     * Determines if the package was delivered on time
     * 
     * @return True if delivered on or before the estimated delivery date
     */
    private boolean isDeliveredOnTime() {
        if (!isDelivered || estimatedDeliveryDate == null || actualDeliveryDate == null) {
            return false;
        }
        
        // Compare dates ignoring time
        return !actualDeliveryDate.after(estimatedDeliveryDate);
    }
    
    /**
     * Calculates the estimated delivery date based on shipping method and carrier
     */
    private void calculateEstimatedDeliveryDate() {
        // Get current date
        Date currentDate = new Date();
        
        // Calculate based on shipping method
        int daysToAdd = 5; // Default for standard shipping
        
        if ("Express".equalsIgnoreCase(shippingMethod)) {
            daysToAdd = 2;
        } else if ("Priority".equalsIgnoreCase(shippingMethod)) {
            daysToAdd = 1;
        } else if ("Economy".equalsIgnoreCase(shippingMethod)) {
            daysToAdd = 7;
        }
        
        // Adjust based on carrier (some carriers are faster)
        if ("FedEx".equals(carrierName) || "DHL".equals(carrierName)) {
            daysToAdd = Math.max(1, daysToAdd - 1);
        }
        
        // International shipping takes longer
        if (!"United States".equalsIgnoreCase(country)) {
            daysToAdd += 5;
            
            // Adjust based on region
            if (isEuropeanCountry(country)) {
                daysToAdd += 2;
            } else if (isAsianCountry(country)) {
                daysToAdd += 4;
            } else {
                daysToAdd += 6; // Other regions
            }
        }
        
        // Calculate the estimated delivery date
        long newTime = currentDate.getTime() + (daysToAdd * 24 * 60 * 60 * 1000);
        this.estimatedDeliveryDate = new Date(newTime);
    }
    
    /**
     * Check if the country is in Europe
     * 
     * @param country The country name
     * @return True if the country is in Europe
     */
    private boolean isEuropeanCountry(String country) {
        // Simple check for common European countries
        String[] europeanCountries = {"Germany", "France", "UK", "Italy", "Spain", 
                                     "Netherlands", "Belgium", "Switzerland", "Austria"};
        
        for (String europeanCountry : europeanCountries) {
            if (europeanCountry.equalsIgnoreCase(country)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Check if the country is in Asia
     * 
     * @param country The country name
     * @return True if the country is in Asia
     */
    private boolean isAsianCountry(String country) {
        // Simple check for common Asian countries
        String[] asianCountries = {"China", "Japan", "South Korea", "India", "Singapore", 
                                  "Thailand", "Vietnam", "Malaysia", "Indonesia"};
        
        for (String asianCountry : asianCountries) {
            if (asianCountry.equalsIgnoreCase(country)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Generates a random tracking number for testing purposes
     * 
     * @return A randomly generated tracking number
     */
    public String generateRandomTrackingNumber() {
        Random random = new Random();
        int carrierIndex = random.nextInt(SHIPPING_PROVIDERS.length);
        String carrier = SHIPPING_PROVIDERS[carrierIndex];
        
        StringBuilder sb = new StringBuilder();
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        
        // Generate random tracking number based on carrier format
        switch (carrier) {
            case "FedEx":
                // Format: 9999 9999 9999
                for (int i = 0; i < 12; i++) {
                    sb.append(random.nextInt(10));
                }
                break;
                
            case "UPS":
                // Format: 1Z 999 999 99 9999 999 9
                sb.append("1Z");
                for (int i = 0; i < 16; i++) {
                    sb.append(random.nextInt(10));
                }
                break;
                
            default:
                // Generic format
                for (int i = 0; i < 10; i++) {
                    sb.append(alphabet.charAt(random.nextInt(alphabet.length())));
                }
        }
        
        this.trackingNumber = sb.toString();
        this.carrierName = carrier;
        
        return this.trackingNumber;
    }
    
    // Getters and setters
    
    public String getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(String shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getZipCode() {
        return zipCode;
    }

    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public String getCarrierName() {
        return carrierName;
    }

    public void setCarrierName(String carrierName) {
        this.carrierName = carrierName;
    }

    public Date getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(Date estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public Date getActualDeliveryDate() {
        return actualDeliveryDate;
    }

    public void setActualDeliveryDate(Date actualDeliveryDate) {
        this.actualDeliveryDate = actualDeliveryDate;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public int getDeliveryAttempts() {
        return deliveryAttempts;
    }

    public void setDeliveryAttempts(int deliveryAttempts) {
        this.deliveryAttempts = deliveryAttempts;
    }

    public String getDeliveryNotes() {
        return deliveryNotes;
    }

    public void setDeliveryNotes(String deliveryNotes) {
        this.deliveryNotes = deliveryNotes;
    }

    public boolean isRequiresSignature() {
        return requiresSignature;
    }

    public void setRequiresSignature(boolean requiresSignature) {
        this.requiresSignature = requiresSignature;
    }

    public boolean isInsured() {
        return isInsured;
    }

    public void setInsured(boolean insured) {
        isInsured = insured;
    }

    public BigDecimal getInsuranceAmount() {
        return insuranceAmount;
    }

    public void setInsuranceAmount(BigDecimal insuranceAmount) {
        this.insuranceAmount = insuranceAmount;
    }

    public boolean isPriority() {
        return isPriority;
    }

    public void setPriority(boolean priority) {
        isPriority = priority;
    }
    
    @Override
    public String toString() {
        return "ShippingDetails{" +
                "shippingAddress='" + shippingAddress + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", zipCode='" + zipCode + '\'' +
                ", country='" + country + '\'' +
                ", shippingMethod='" + shippingMethod + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                ", carrierName='" + carrierName + '\'' +
                ", isDelivered=" + isDelivered +
                '}';
    }
}