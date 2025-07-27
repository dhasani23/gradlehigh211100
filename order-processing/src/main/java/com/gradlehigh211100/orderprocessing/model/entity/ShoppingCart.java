package com.gradlehigh211100.orderprocessing.model.entity;

import com.gradlehigh211100.common.model.BaseEntity;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity representing a user's shopping cart with cart items, session management, and cart persistence capabilities.
 * This class handles the state and operations related to a user's shopping cart during their shopping session.
 * 
 * The shopping cart maintains a list of cart items, tracks session information for both authenticated and
 * anonymous users, and handles cart state transitions based on user interactions.
 */
public class ShoppingCart extends BaseEntity {

    private static final Logger LOGGER = Logger.getLogger(ShoppingCart.class.getName());
    
    // Thread safety for concurrent cart operations
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    // User identification fields
    private Long userId;
    private String sessionId;
    
    // Cart contents
    private List<CartItem> cartItems;
    
    // Timestamps
    private Date createdDate;
    private Date lastModifiedDate;
    
    // Status flag
    private Boolean isActive;

    /**
     * Default constructor initializes an empty cart with default values.
     */
    public ShoppingCart() {
        this.cartItems = new ArrayList<>();
        this.createdDate = new Date();
        this.lastModifiedDate = new Date();
        this.isActive = true;
    }

    /**
     * Constructor for creating a cart for an authenticated user.
     *
     * @param userId the ID of the user who owns this cart
     */
    public ShoppingCart(Long userId) {
        this();
        this.userId = userId;
        LOGGER.log(Level.INFO, "Created new cart for user ID: {0}", userId);
    }

    /**
     * Constructor for creating a cart for an anonymous session.
     *
     * @param sessionId the session identifier for tracking anonymous users
     */
    public ShoppingCart(String sessionId) {
        this();
        this.sessionId = sessionId;
        LOGGER.log(Level.INFO, "Created new cart for session ID: {0}", sessionId);
    }

    /**
     * Full constructor with all fields.
     */
    public ShoppingCart(Long userId, String sessionId, List<CartItem> cartItems, 
                        Date createdDate, Date lastModifiedDate, Boolean isActive) {
        this.userId = userId;
        this.sessionId = sessionId;
        this.cartItems = cartItems != null ? cartItems : new ArrayList<>();
        this.createdDate = createdDate != null ? createdDate : new Date();
        this.lastModifiedDate = lastModifiedDate != null ? lastModifiedDate : new Date();
        this.isActive = isActive != null ? isActive : true;
    }

    /**
     * Adds an item to the cart or updates quantity if the product already exists in the cart.
     * This method implements complex business logic for inventory checking and quantity validation.
     *
     * @param productId the ID of the product to add
     * @param quantity the quantity to add
     * @throws IllegalArgumentException if quantity is invalid or product doesn't exist
     */
    public void addItem(Long productId, Integer quantity) {
        if (productId == null) {
            LOGGER.log(Level.WARNING, "Attempt to add null product ID to cart");
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        
        if (quantity == null || quantity <= 0) {
            LOGGER.log(Level.WARNING, "Invalid quantity {0} for product ID: {1}", new Object[]{quantity, productId});
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }

        lock.writeLock().lock();
        try {
            Optional<CartItem> existingItem = findCartItemByProductId(productId);
            
            if (existingItem.isPresent()) {
                // Update existing item
                CartItem item = existingItem.get();
                
                // Complex business logic for inventory validation
                if (validateInventoryForUpdate(productId, item.getQuantity() + quantity)) {
                    int newQuantity = item.getQuantity() + quantity;
                    item.setQuantity(newQuantity);
                    LOGGER.log(Level.FINE, "Updated quantity for product ID: {0}, new quantity: {1}", 
                              new Object[]{productId, newQuantity});
                } else {
                    LOGGER.log(Level.WARNING, "Insufficient inventory for product ID: {0}, requested quantity: {1}", 
                              new Object[]{productId, quantity});
                    throw new IllegalStateException("Insufficient inventory for requested quantity");
                }
            } else {
                // Add new item
                if (validateInventoryForAdd(productId, quantity)) {
                    CartItem newItem = createNewCartItem(productId, quantity);
                    cartItems.add(newItem);
                    LOGGER.log(Level.FINE, "Added new item to cart - product ID: {0}, quantity: {1}", 
                              new Object[]{productId, quantity});
                } else {
                    LOGGER.log(Level.WARNING, "Insufficient inventory for product ID: {0}, requested quantity: {1}", 
                              new Object[]{productId, quantity});
                    throw new IllegalStateException("Insufficient inventory for requested quantity");
                }
            }
            
            updateLastModifiedDate();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error adding item to cart: " + e.getMessage(), e);
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes an item from the cart based on product ID.
     *
     * @param productId the ID of the product to remove
     */
    public void removeItem(Long productId) {
        if (productId == null) {
            LOGGER.log(Level.WARNING, "Attempt to remove item with null product ID");
            return;
        }

        lock.writeLock().lock();
        try {
            boolean removed = false;
            Iterator<CartItem> iterator = cartItems.iterator();
            
            while (iterator.hasNext()) {
                CartItem item = iterator.next();
                if (Objects.equals(item.getProductId(), productId)) {
                    iterator.remove();
                    removed = true;
                    break;
                }
            }
            
            if (removed) {
                LOGGER.log(Level.FINE, "Removed item with product ID: {0}", productId);
                updateLastModifiedDate();
            } else {
                LOGGER.log(Level.WARNING, "No item found with product ID: {0} to remove", productId);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error removing item from cart: " + e.getMessage(), e);
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Updates the quantity of a specific item in the cart.
     * 
     * @param productId the ID of the product to update
     * @param quantity the new quantity for the item
     * @throws IllegalArgumentException if the quantity is invalid or the product doesn't exist in the cart
     */
    public void updateItemQuantity(Long productId, Integer quantity) {
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        
        if (quantity == null || quantity < 0) {
            throw new IllegalArgumentException("Quantity cannot be negative");
        }
        
        // If quantity is 0, simply remove the item
        if (quantity == 0) {
            removeItem(productId);
            return;
        }

        lock.writeLock().lock();
        try {
            Optional<CartItem> existingItem = findCartItemByProductId(productId);
            
            if (existingItem.isPresent()) {
                CartItem item = existingItem.get();
                
                // Complex business logic for inventory validation
                if (validateInventoryForUpdate(productId, quantity)) {
                    item.setQuantity(quantity);
                    LOGGER.log(Level.FINE, "Updated quantity for product ID: {0}, new quantity: {1}", 
                              new Object[]{productId, quantity});
                } else {
                    LOGGER.log(Level.WARNING, "Insufficient inventory for product ID: {0}, requested quantity: {1}", 
                              new Object[]{productId, quantity});
                    throw new IllegalStateException("Insufficient inventory for requested quantity");
                }
                
                updateLastModifiedDate();
            } else {
                LOGGER.log(Level.WARNING, "Attempted to update quantity for non-existent product ID: {0}", productId);
                throw new IllegalArgumentException("No item with product ID " + productId + " found in cart");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error updating item quantity: " + e.getMessage(), e);
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Clears all items from the cart.
     */
    public void clearCart() {
        lock.writeLock().lock();
        try {
            // Capture count before clearing for logging
            int itemCount = cartItems.size();
            
            // Clear items and update timestamp
            cartItems.clear();
            updateLastModifiedDate();
            
            LOGGER.log(Level.INFO, "Cleared cart, removed {0} items", itemCount);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error clearing cart: " + e.getMessage(), e);
            throw e;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the total number of items in the cart.
     * This method counts the sum of quantities across all cart items.
     *
     * @return the total number of items
     */
    public Integer getTotalItems() {
        lock.readLock().lock();
        try {
            int total = 0;
            
            // Calculate sum of quantities from all cart items
            for (CartItem item : cartItems) {
                // Handle possible null quantity
                Integer quantity = item.getQuantity();
                if (quantity != null) {
                    total += quantity;
                }
            }
            
            return total;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error calculating total items: " + e.getMessage(), e);
            throw e;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Checks if the cart is empty.
     *
     * @return true if the cart has no items, false otherwise
     */
    public boolean isEmpty() {
        lock.readLock().lock();
        try {
            return cartItems.isEmpty();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Finds a cart item by product ID.
     *
     * @param productId the product ID to search for
     * @return an Optional containing the cart item if found, empty otherwise
     */
    private Optional<CartItem> findCartItemByProductId(Long productId) {
        for (CartItem item : cartItems) {
            if (Objects.equals(item.getProductId(), productId)) {
                return Optional.of(item);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a new cart item.
     * This is a helper method used by addItem.
     *
     * @param productId the ID of the product
     * @param quantity the quantity of the item
     * @return a new CartItem instance
     */
    private CartItem createNewCartItem(Long productId, Integer quantity) {
        // FIXME: This method needs to fetch product information from a product service
        CartItem item = new CartItem();
        item.setProductId(productId);
        item.setQuantity(quantity);
        // TODO: Set additional cart item properties like price from product service
        return item;
    }

    /**
     * Validates inventory for adding a new item.
     * This is a placeholder for inventory validation logic.
     *
     * @param productId the ID of the product
     * @param quantity the quantity to check against inventory
     * @return true if inventory is sufficient, false otherwise
     */
    private boolean validateInventoryForAdd(Long productId, Integer quantity) {
        // TODO: Implement actual inventory validation logic by calling inventory service
        // This is a placeholder implementation
        return true;
    }

    /**
     * Validates inventory for updating an existing item.
     * This is a placeholder for inventory validation logic.
     *
     * @param productId the ID of the product
     * @param newQuantity the new quantity to check against inventory
     * @return true if inventory is sufficient, false otherwise
     */
    private boolean validateInventoryForUpdate(Long productId, Integer newQuantity) {
        // TODO: Implement actual inventory validation logic by calling inventory service
        // This is a placeholder implementation
        return true;
    }

    /**
     * Updates the last modified date to the current time.
     */
    private void updateLastModifiedDate() {
        this.lastModifiedDate = new Date();
    }

    // Getters and setters

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public List<CartItem> getCartItems() {
        // Return a defensive copy to prevent external modification
        lock.readLock().lock();
        try {
            return new ArrayList<>(cartItems);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void setCartItems(List<CartItem> cartItems) {
        lock.writeLock().lock();
        try {
            this.cartItems = cartItems != null ? new ArrayList<>(cartItems) : new ArrayList<>();
            updateLastModifiedDate();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public Date getCreatedDate() {
        return createdDate != null ? new Date(createdDate.getTime()) : null;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate != null ? new Date(createdDate.getTime()) : null;
    }

    public Date getLastModifiedDate() {
        return lastModifiedDate != null ? new Date(lastModifiedDate.getTime()) : null;
    }

    public void setLastModifiedDate(Date lastModifiedDate) {
        this.lastModifiedDate = lastModifiedDate != null ? new Date(lastModifiedDate.getTime()) : null;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ShoppingCart)) return false;
        if (!super.equals(o)) return false;
        
        ShoppingCart that = (ShoppingCart) o;
        return Objects.equals(userId, that.userId) &&
               Objects.equals(sessionId, that.sessionId) &&
               Objects.equals(isActive, that.isActive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), userId, sessionId, isActive);
    }

    @Override
    public String toString() {
        return "ShoppingCart{" +
               "id=" + getId() +
               ", userId=" + userId +
               ", sessionId='" + sessionId + '\'' +
               ", itemCount=" + (cartItems != null ? cartItems.size() : 0) +
               ", isActive=" + isActive +
               ", createdDate=" + createdDate +
               ", lastModifiedDate=" + lastModifiedDate +
               '}';
    }
}