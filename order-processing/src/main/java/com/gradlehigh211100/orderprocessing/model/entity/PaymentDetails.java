package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;
import com.gradlehigh211100.orderprocessing.model.enums.PaymentMethod;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

/**
 * Contains payment details for an order including payment method,
 * transaction information, and payment status.
 */
public class PaymentDetails extends BaseEntity {
    
    private PaymentMethod method;
    private String transactionId;
    private Date paymentDate;
    private Boolean paid;
    private String cardNumber;
    private Date expirationDate;
    private String cardholderName;
    private String paypalEmail;
    private String bankAccountNumber;
    private String bankRoutingNumber;
    private Date refundDate;
    private BigDecimal refundAmount;
    
    /**
     * Default constructor
     */
    public PaymentDetails() {
        this.paid = false;
    }
    
    /**
     * Constructor with payment method
     * 
     * @param method the payment method used
     */
    public PaymentDetails(PaymentMethod method) {
        this();
        this.method = method;
    }
    
    /**
     * Full constructor for payment details
     * 
     * @param method the payment method used
     * @param transactionId the payment transaction ID
     * @param paymentDate the date payment was processed
     * @param paid whether payment has been received
     */
    public PaymentDetails(PaymentMethod method, String transactionId, Date paymentDate, Boolean paid) {
        this.method = method;
        this.transactionId = transactionId;
        this.paymentDate = paymentDate;
        this.paid = paid;
    }
    
    /**
     * Validates that the payment details are complete and valid
     * based on the payment method
     * 
     * @return true if the payment details are valid, false otherwise
     */
    public boolean isValid() {
        if (method == null) {
            return false;
        }
        
        switch (method) {
            case CREDIT_CARD:
                return cardNumber != null && expirationDate != null && cardholderName != null;
            case PAYPAL:
                return paypalEmail != null && !paypalEmail.isEmpty();
            case BANK_TRANSFER:
                return bankAccountNumber != null && bankRoutingNumber != null;
            case COD:
                return true; // No additional details needed
            case GIFT_CARD:
            case STORE_CREDIT:
                return transactionId != null;
            default:
                return false;
        }
    }
    
    /**
     * Checks if payment has been received and verified
     * 
     * @return true if payment is complete, false otherwise
     */
    public boolean isPaid() {
        return Boolean.TRUE.equals(paid);
    }

    // Getters and setters
    
    public PaymentMethod getMethod() {
        return method;
    }

    public void setMethod(PaymentMethod method) {
        this.method = method;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public Date getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(Date paymentDate) {
        this.paymentDate = paymentDate;
    }

    public Boolean getPaid() {
        return paid;
    }

    public void setPaid(Boolean paid) {
        this.paid = paid;
    }

    public String getCardNumber() {
        return cardNumber;
    }

    public void setCardNumber(String cardNumber) {
        this.cardNumber = cardNumber;
    }

    public Date getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getCardholderName() {
        return cardholderName;
    }

    public void setCardholderName(String cardholderName) {
        this.cardholderName = cardholderName;
    }

    public String getPaypalEmail() {
        return paypalEmail;
    }

    public void setPaypalEmail(String paypalEmail) {
        this.paypalEmail = paypalEmail;
    }

    public String getBankAccountNumber() {
        return bankAccountNumber;
    }

    public void setBankAccountNumber(String bankAccountNumber) {
        this.bankAccountNumber = bankAccountNumber;
    }

    public String getBankRoutingNumber() {
        return bankRoutingNumber;
    }

    public void setBankRoutingNumber(String bankRoutingNumber) {
        this.bankRoutingNumber = bankRoutingNumber;
    }

    public Date getRefundDate() {
        return refundDate;
    }

    public void setRefundDate(Date refundDate) {
        this.refundDate = refundDate;
    }

    public BigDecimal getRefundAmount() {
        return refundAmount;
    }

    public void setRefundAmount(BigDecimal refundAmount) {
        this.refundAmount = refundAmount;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentDetails)) return false;
        if (!super.equals(o)) return false;
        
        PaymentDetails that = (PaymentDetails) o;
        
        if (!Objects.equals(transactionId, that.transactionId)) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (transactionId != null ? transactionId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PaymentDetails{" +
                "id=" + getId() +
                ", method=" + method +
                ", transactionId='" + transactionId + '\'' +
                ", paid=" + paid +
                ", paymentDate=" + paymentDate +
                '}';
    }
}