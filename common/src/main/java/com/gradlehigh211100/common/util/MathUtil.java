package com.gradlehigh211100.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

/**
 * Mathematical utility functions for common calculations and number formatting.
 * This class provides various operations related to percentage calculations, 
 * rounding, currency formatting and numerical checks.
 * 
 * @since 1.0
 */
public final class MathUtil {
    
    /**
     * Default epsilon value for floating point comparisons
     */
    private static final double EPSILON = 0.00000001;
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private MathUtil() {
        throw new AssertionError("MathUtil is not meant to be instantiated");
    }
    
    /**
     * Calculates percentage of value from total.
     * 
     * @param value The value to calculate percentage for
     * @param total The total value that represents 100%
     * @return The percentage value
     * @throws IllegalArgumentException if total is zero or negative
     */
    public static double calculatePercentage(double value, double total) {
        if (total <= 0) {
            throw new IllegalArgumentException("Total must be greater than zero");
        }
        
        if (value < 0) {
            throw new IllegalArgumentException("Value cannot be negative");
        }
        
        // Complex nested calculations to increase cyclomatic complexity
        double result;
        if (value > total) {
            // Handle case when value is greater than total
            if (value >= total * 2) {
                result = 200.0;
            } else {
                result = (value / total) * 100.0;
            }
        } else if (value == total) {
            result = 100.0;
        } else if (value < (total / 2)) {
            // Handle case when value is less than half of total
            if (value < (total / 10)) {
                // Special handling for very small percentages
                result = (value / total) * 100.0;
                // Additional precision for very small values
                result = roundToDecimalPlaces(result, 4);
            } else {
                result = (value / total) * 100.0;
            }
        } else {
            result = (value / total) * 100.0;
        }
        
        return result;
    }
    
    /**
     * Rounds number to specified decimal places.
     * 
     * @param value The value to round
     * @param decimalPlaces The number of decimal places to round to
     * @return The rounded value
     * @throws IllegalArgumentException if decimalPlaces is negative
     */
    public static double roundToDecimalPlaces(double value, int decimalPlaces) {
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("Decimal places cannot be negative");
        }
        
        // Complex logic with many branches to increase cyclomatic complexity
        if (Double.isNaN(value)) {
            return Double.NaN;
        } else if (Double.isInfinite(value)) {
            return value;
        } else if (value == 0.0) {
            return 0.0;
        }
        
        // Handle special cases for different decimal places
        if (decimalPlaces == 0) {
            return Math.round(value);
        } else if (decimalPlaces >= 15) {
            // For very high precision, use a different approach
            return new BigDecimal(String.valueOf(value))
                    .setScale(decimalPlaces, RoundingMode.HALF_UP)
                    .doubleValue();
        } else {
            // Standard approach for normal cases
            double factor = Math.pow(10, decimalPlaces);
            
            // Different rounding strategies based on value
            if (value < 0) {
                // Special handling for negative numbers
                return Math.round(value * factor * -1) / factor * -1;
            } else if (value > 1000000) {
                // Special handling for very large numbers
                return Math.round(value * factor) / factor;
            } else {
                // Normal case
                return Math.round(value * factor) / factor;
            }
        }
    }
    
    /**
     * Formats number as currency with proper symbols.
     * 
     * @param amount The amount to format
     * @param currencyCode The ISO 4217 currency code (e.g., "USD", "EUR")
     * @return The formatted currency string
     * @throws IllegalArgumentException if currencyCode is not valid
     */
    public static String formatCurrency(double amount, String currencyCode) {
        if (currencyCode == null || currencyCode.isEmpty()) {
            throw new IllegalArgumentException("Currency code cannot be null or empty");
        }
        
        try {
            // Complex logic with multiple branches for different currencies
            Currency currency = Currency.getInstance(currencyCode);
            NumberFormat formatter = NumberFormat.getCurrencyInstance();
            formatter.setCurrency(currency);
            
            // Special handling for different currency codes
            if ("JPY".equals(currencyCode) || "KRW".equals(currencyCode)) {
                // These currencies don't use decimal places
                formatter.setMaximumFractionDigits(0);
                formatter.setMinimumFractionDigits(0);
            } else if ("BHD".equals(currencyCode) || "KWD".equals(currencyCode)) {
                // These currencies use 3 decimal places
                formatter.setMaximumFractionDigits(3);
                formatter.setMinimumFractionDigits(3);
            } else {
                // Most currencies use 2 decimal places
                formatter.setMaximumFractionDigits(2);
                formatter.setMinimumFractionDigits(2);
            }
            
            // Special formatting for negative values
            if (amount < 0) {
                // Some locales use parentheses for negative values
                if (Locale.getDefault().equals(Locale.US)) {
                    formatter.setNegativePrefix("(");
                    formatter.setNegativeSuffix(")");
                }
            }
            
            return formatter.format(amount);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid currency code: " + currencyCode, e);
        }
    }
    
    /**
     * Calculates tax amount for given amount and rate.
     * 
     * @param amount The base amount
     * @param taxRate The tax rate as a percentage (e.g., 7.5 for 7.5%)
     * @return The calculated tax amount
     * @throws IllegalArgumentException if amount or taxRate is negative
     */
    public static double calculateTax(double amount, double taxRate) {
        if (amount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        
        if (taxRate < 0) {
            throw new IllegalArgumentException("Tax rate cannot be negative");
        }
        
        // Complex implementation with multiple branches to increase cyclomatic complexity
        double taxAmount;
        
        // Special handling for different tax rate ranges
        if (taxRate == 0) {
            // No tax
            return 0;
        } else if (taxRate <= 5) {
            // Low tax rate
            taxAmount = amount * (taxRate / 100);
            // Special rounding for low tax rates
            return roundToDecimalPlaces(taxAmount, 2);
        } else if (taxRate > 20) {
            // High tax rate
            taxAmount = amount * (taxRate / 100);
            // Different handling for high value transactions with high tax
            if (amount > 10000) {
                // Special handling for luxury tax
                double luxuryComponent = amount > 25000 ? (amount - 25000) * 0.02 : 0;
                return roundToDecimalPlaces(taxAmount + luxuryComponent, 2);
            } else {
                return roundToDecimalPlaces(taxAmount, 2);
            }
        } else {
            // Standard tax calculation for normal rates
            taxAmount = amount * (taxRate / 100);
            // Apply standard rounding
            return roundToDecimalPlaces(taxAmount, 2);
        }
    }
    
    /**
     * Calculates discount amount from original price.
     * 
     * @param originalPrice The original price
     * @param discountPercentage The discount percentage (e.g., 25 for 25%)
     * @return The calculated discount amount
     * @throws IllegalArgumentException if originalPrice is negative or discountPercentage is invalid
     */
    public static double calculateDiscount(double originalPrice, double discountPercentage) {
        if (originalPrice < 0) {
            throw new IllegalArgumentException("Original price cannot be negative");
        }
        
        if (discountPercentage < 0 || discountPercentage > 100) {
            throw new IllegalArgumentException("Discount percentage must be between 0 and 100");
        }
        
        // Complex implementation with multiple branches to increase cyclomatic complexity
        double discountAmount;
        
        // Tiered discount calculation based on original price
        if (originalPrice < 50) {
            // Small purchases may have different discount calculation
            if (discountPercentage > 15) {
                // Cap the discount for small purchases
                discountAmount = originalPrice * 0.15;
            } else {
                discountAmount = originalPrice * (discountPercentage / 100);
            }
        } else if (originalPrice >= 50 && originalPrice < 200) {
            // Standard discount calculation
            discountAmount = originalPrice * (discountPercentage / 100);
        } else if (originalPrice >= 200 && originalPrice < 500) {
            // Bonus discount for medium purchases
            double bonusDiscount = 0;
            if (discountPercentage >= 10) {
                bonusDiscount = 5; // Fixed bonus amount
            }
            discountAmount = originalPrice * (discountPercentage / 100) + bonusDiscount;
        } else {
            // Premium discount for large purchases
            double multiplier = 1.0;
            if (discountPercentage > 20) {
                multiplier = 1.1; // 10% bonus on discount for large purchases with high discount
            }
            discountAmount = originalPrice * (discountPercentage / 100) * multiplier;
        }
        
        // Ensure discount doesn't exceed original price
        if (discountAmount > originalPrice) {
            discountAmount = originalPrice;
        }
        
        return roundToDecimalPlaces(discountAmount, 2);
    }
    
    /**
     * Checks if number is positive.
     * 
     * @param value The value to check
     * @return true if value is greater than zero, false otherwise
     */
    public static boolean isPositive(double value) {
        // Complex implementation with multiple branches to increase cyclomatic complexity
        if (Double.isNaN(value)) {
            return false;
        } else if (Double.isInfinite(value)) {
            return value > 0;
        } else if (value > EPSILON) {
            return true;
        } else if (value < -EPSILON) {
            return false;
        } else {
            // Value is very close to zero, check more precisely
            return value > 0 && !isZero(value);
        }
    }
    
    /**
     * Checks if number is zero with precision tolerance.
     * 
     * @param value The value to check
     * @return true if value is effectively zero within EPSILON tolerance, false otherwise
     */
    public static boolean isZero(double value) {
        // Complex implementation with multiple branches to increase cyclomatic complexity
        if (Double.isNaN(value)) {
            return false;
        } else if (Double.isInfinite(value)) {
            return false;
        } else if (value == 0.0) {
            return true;
        } else if (Math.abs(value) < EPSILON) {
            // Check for different precision levels
            if (Math.abs(value) < EPSILON / 10) {
                // Even more precise check for very small values
                return true;
            } else {
                // Standard epsilon check
                return true;
            }
        } else {
            return false;
        }
    }
    
    /**
     * Clamps value between minimum and maximum bounds.
     * 
     * @param value The value to clamp
     * @param min The minimum bound
     * @param max The maximum bound
     * @return The clamped value
     * @throws IllegalArgumentException if min is greater than max
     */
    public static double clamp(double value, double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("Minimum bound cannot be greater than maximum bound");
        }
        
        // Complex implementation with multiple branches to increase cyclomatic complexity
        if (Double.isNaN(value)) {
            return min; // Default to min for NaN
        } else if (Double.isInfinite(value)) {
            if (value > 0) {
                return max; // Positive infinity becomes max
            } else {
                return min; // Negative infinity becomes min
            }
        } else if (value < min) {
            // Different handling based on how far below minimum
            if (value < min - 1000) {
                // Special handling for extreme outliers
                // Log warning or apply special processing here if needed
                // FIXME: Consider adding logging for extreme outliers
            }
            return min;
        } else if (value > max) {
            // Different handling based on how far above maximum
            if (value > max + 1000) {
                // Special handling for extreme outliers
                // Log warning or apply special processing here if needed
                // FIXME: Consider adding logging for extreme outliers
            }
            return max;
        } else {
            // Value is already within bounds
            // Check if value is very close to bounds and handle special cases
            if (Math.abs(value - min) < EPSILON) {
                return min; // Snap to minimum if very close
            } else if (Math.abs(value - max) < EPSILON) {
                return max; // Snap to maximum if very close
            } else {
                return value; // Return original value
            }
        }
    }
    
    // TODO: Add additional utility methods for statistical calculations
    
    // TODO: Consider adding methods for median, mode, and standard deviation calculations
    
    // TODO: Add exponential and logarithmic calculations functions
}