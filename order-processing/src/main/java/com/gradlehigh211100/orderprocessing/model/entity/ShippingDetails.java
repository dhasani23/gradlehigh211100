package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;

import java.util.Date;
import java.util.Objects;

/**
 * Contains shipping details for an order including address information,
 * shipping provider, tracking information, and delivery status.
 */
public class ShippingDetails extends BaseEntity {
    
    private String recipientName;
    private String street;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String phoneNumber;
    private String shippingMethod;
    private String shippingProvider;
    private String trackingNumber;
    private Date estimatedDeliveryDate;
    private Date shippedDate;
    private Date deliveredDate;
    private Date returnInitiatedDate;
    private Boolean businessAddress;
    private Boolean taxExempt;
    
    /**
     * Default constructor
     */
    public ShippingDetails() {
        this.businessAddress = false;
        this.taxExempt = false;
    }
    
    /**
     * Constructor with basic address details
     * 
     * @param recipientName name of recipient
     * @param street street address
     * @param city city name
     * @param state state or province
     * @param postalCode postal or zip code
     * @param country country name
     */
    public ShippingDetails(String recipientName, String street, String city, 
            String state, String postalCode, String country) {
        this();
        this.recipientName = recipientName;
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
    }
    
    /**
     * Full constructor with shipping and delivery details
     * 
     * @param recipientName name of recipient
     * @param street street address
     * @param city city name
     * @param state state or province
     * @param postalCode postal or zip code
     * @param country country name
     * @param phoneNumber contact phone number
     * @param shippingMethod method of shipping (Standard, Express, etc.)
     * @param shippingProvider shipping carrier (UPS, FedEx, etc.)
     * @param trackingNumber shipping tracking ID
     * @param estimatedDeliveryDate estimated delivery date
     * @param businessAddress whether this is a business address
     * @param taxExempt whether this shipment is tax exempt
     */
    public ShippingDetails(String recipientName, String street, String city, 
            String state, String postalCode, String country, String phoneNumber,
            String shippingMethod, String shippingProvider, String trackingNumber,
            Date estimatedDeliveryDate, Boolean businessAddress, Boolean taxExempt) {
        
        this.recipientName = recipientName;
        this.street = street;
        this.city = city;
        this.state = state;
        this.postalCode = postalCode;
        this.country = country;
        this.phoneNumber = phoneNumber;
        this.shippingMethod = shippingMethod;
        this.shippingProvider = shippingProvider;
        this.trackingNumber = trackingNumber;
        this.estimatedDeliveryDate = estimatedDeliveryDate;
        this.businessAddress = businessAddress;
        this.taxExempt = taxExempt;
    }
    
    /**
     * Gets the full formatted address as a string
     * 
     * @return formatted address
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        
        if (recipientName != null && !recipientName.isEmpty()) {
            sb.append(recipientName).append("\n");
        }
        
        if (street != null && !street.isEmpty()) {
            sb.append(street).append("\n");
        }
        
        if (city != null && !city.isEmpty()) {
            sb.append(city);
            
            if (state != null && !state.isEmpty()) {
                sb.append(", ").append(state);
            }
            
            if (postalCode != null && !postalCode.isEmpty()) {
                sb.append(" ").append(postalCode);
            }
            
            sb.append("\n");
        }
        
        if (country != null && !country.isEmpty()) {
            sb.append(country);
        }
        
        return sb.toString();
    }
    
    /**
     * Gets the region (state/province)
     * 
     * @return the region
     */
    public String getRegion() {
        return state;
    }
    
    /**
     * Checks if this is a business address
     * 
     * @return true if business address, false if residential
     */
    public boolean isBusinessAddress() {
        return Boolean.TRUE.equals(businessAddress);
    }
    
    /**
     * Checks if this shipment is tax exempt
     * 
     * @return true if tax exempt, false otherwise
     */
    public Boolean getTaxExempt() {
        return taxExempt;
    }

    // Getters and setters
    
    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getStreet() {
        return street;
    }

    public void setStreet(String street) {
        this.street = street;
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

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getShippingMethod() {
        return shippingMethod;
    }

    public void setShippingMethod(String shippingMethod) {
        this.shippingMethod = shippingMethod;
    }

    public String getShippingProvider() {
        return shippingProvider;
    }

    public void setShippingProvider(String shippingProvider) {
        this.shippingProvider = shippingProvider;
    }

    public String getTrackingNumber() {
        return trackingNumber;
    }

    public void setTrackingNumber(String trackingNumber) {
        this.trackingNumber = trackingNumber;
    }

    public Date getEstimatedDeliveryDate() {
        return estimatedDeliveryDate;
    }

    public void setEstimatedDeliveryDate(Date estimatedDeliveryDate) {
        this.estimatedDeliveryDate = estimatedDeliveryDate;
    }

    public Date getShippedDate() {
        return shippedDate;
    }

    public void setShippedDate(Date shippedDate) {
        this.shippedDate = shippedDate;
    }

    public Date getDeliveredDate() {
        return deliveredDate;
    }

    public void setDeliveredDate(Date deliveredDate) {
        this.deliveredDate = deliveredDate;
    }

    public Date getReturnInitiatedDate() {
        return returnInitiatedDate;
    }

    public void setReturnInitiatedDate(Date returnInitiatedDate) {
        this.returnInitiatedDate = returnInitiatedDate;
    }

    public void setBusinessAddress(Boolean businessAddress) {
        this.businessAddress = businessAddress;
    }

    public void setTaxExempt(Boolean taxExempt) {
        this.taxExempt = taxExempt;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShippingDetails)) return false;
        if (!super.equals(o)) return false;
        
        ShippingDetails that = (ShippingDetails) o;
        
        if (!Objects.equals(recipientName, that.recipientName)) return false;
        if (!Objects.equals(street, that.street)) return false;
        if (!Objects.equals(city, that.city)) return false;
        if (!Objects.equals(state, that.state)) return false;
        if (!Objects.equals(postalCode, that.postalCode)) return false;
        if (!Objects.equals(country, that.country)) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (recipientName != null ? recipientName.hashCode() : 0);
        result = 31 * result + (street != null ? street.hashCode() : 0);
        result = 31 * result + (city != null ? city.hashCode() : 0);
        result = 31 * result + (state != null ? state.hashCode() : 0);
        result = 31 * result + (postalCode != null ? postalCode.hashCode() : 0);
        result = 31 * result + (country != null ? country.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ShippingDetails{" +
                "id=" + getId() +
                ", recipientName='" + recipientName + '\'' +
                ", city='" + city + '\'' +
                ", state='" + state + '\'' +
                ", country='" + country + '\'' +
                ", trackingNumber='" + trackingNumber + '\'' +
                '}';
    }
}