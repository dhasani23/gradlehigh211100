package com.gradlehigh211100.productcatalog.service;

import com.gradlehigh211100.productcatalog.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Image service providing business logic for product image management, upload, processing, and optimization.
 */
@Service
public class ImageService {

    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);
    
    private final ProductRepository productRepository;
    private final String imageStoragePath;
    private final List<String> allowedImageTypes;
    
    private static final int THUMBNAIL_WIDTH = 150;
    private static final int THUMBNAIL_HEIGHT = 150;
    private static final String THUMBNAIL_PREFIX = "thumb_";
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024; // 5MB

    @Autowired
    public ImageService(
            ProductRepository productRepository,
            @Value("${app.image.storage-path:/tmp/product-images}") String imageStoragePath,
            @Value("${app.image.allowed-types:image/jpeg,image/png,image/gif}") String allowedTypes) {
        this.productRepository = productRepository;
        this.imageStoragePath = imageStoragePath;
        this.allowedImageTypes = Arrays.asList(allowedTypes.split(","));
        
        // Ensure storage directory exists
        initializeStorageDirectory();
    }
    
    /**
     * Initialize the image storage directory if it doesn't exist
     */
    private void initializeStorageDirectory() {
        try {
            Path storagePath = Paths.get(imageStoragePath);
            if (!Files.exists(storagePath)) {
                Files.createDirectories(storagePath);
                logger.info("Created image storage directory: {}", imageStoragePath);
            }
        } catch (IOException e) {
            logger.error("Failed to create image storage directory: {}", imageStoragePath, e);
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    /**
     * Upload and process product image
     * 
     * @param productId ID of the product to attach image to
     * @param imageFile The image file to be uploaded
     * @return URL of the uploaded image
     * @throws IllegalArgumentException if product not found or image invalid
     * @throws IOException if image processing fails
     */
    public String uploadImage(Long productId, MultipartFile imageFile) {
        // Validate product existence
        if (productId == null || !productRepository.existsById(productId)) {
            logger.error("Product not found for ID: {}", productId);
            throw new IllegalArgumentException("Product not found for ID: " + productId);
        }
        
        // Validate image
        if (!validateImage(imageFile)) {
            logger.error("Invalid image file: {}", imageFile.getOriginalFilename());
            throw new IllegalArgumentException("Invalid image file: " + imageFile.getOriginalFilename());
        }
        
        try {
            // Generate unique filename to prevent collisions
            String originalFilename = imageFile.getOriginalFilename();
            String fileExtension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;
            
            // Prepare file path
            Path destinationFile = Paths.get(imageStoragePath).resolve(uniqueFilename).normalize();
            
            // Save the file
            Files.copy(imageFile.getInputStream(), destinationFile, StandardCopyOption.REPLACE_EXISTING);
            
            // Generate thumbnail
            String imageUrl = "/images/" + uniqueFilename;
            String thumbnailUrl = generateThumbnail(imageUrl);
            
            // Update product with image info (this would be expanded in a real implementation)
            // FIXME: Implement proper image-product relationship in database
            
            logger.info("Successfully uploaded image for product {}: {}", productId, imageUrl);
            
            return imageUrl;
        } catch (IOException e) {
            logger.error("Failed to upload image for product {}", productId, e);
            throw new RuntimeException("Failed to upload image", e);
        }
    }

    /**
     * Delete product image
     * 
     * @param imageId ID of the image to delete
     * @throws IllegalArgumentException if image not found
     * @throws IOException if deletion fails
     */
    public void deleteImage(Long imageId) {
        // FIXME: Implement proper image repository and entity
        if (imageId == null) {
            throw new IllegalArgumentException("Image ID cannot be null");
        }
        
        try {
            // In a real implementation, we would:
            // 1. Retrieve image path from database
            // 2. Delete the physical file
            // 3. Remove database record
            // 4. Update product references
            
            // For demonstration, we'll just check if the deletion would succeed
            boolean imageExists = false; // This would be a database query
            boolean thumbnailExists = false; // This would be a database query
            
            if (!imageExists) {
                logger.error("Image not found for ID: {}", imageId);
                throw new IllegalArgumentException("Image not found for ID: " + imageId);
            }
            
            // Mock file paths - in real implementation these would come from the database
            String imagePath = imageStoragePath + "/some-image-file.jpg";
            String thumbnailPath = imageStoragePath + "/thumb_some-image-file.jpg";
            
            // Delete files
            boolean originalDeleted = new File(imagePath).delete();
            boolean thumbnailDeleted = new File(thumbnailPath).delete();
            
            if (!originalDeleted || !thumbnailDeleted) {
                logger.warn("Failed to delete one or more image files for image ID: {}", imageId);
            }
            
            logger.info("Successfully deleted image with ID: {}", imageId);
        } catch (Exception e) {
            logger.error("Error deleting image with ID: {}", imageId, e);
            throw new RuntimeException("Failed to delete image", e);
        }
    }

    /**
     * Resize image to specified dimensions
     * 
     * @param imageData The raw image data as byte array
     * @param width Target width in pixels
     * @param height Target height in pixels
     * @return Resized image as byte array
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IOException if image processing fails
     */
    public byte[] resizeImage(byte[] imageData, Integer width, Integer height) {
        if (imageData == null || imageData.length == 0) {
            throw new IllegalArgumentException("Image data cannot be null or empty");
        }
        
        if (width == null || width <= 0 || height == null || height <= 0) {
            throw new IllegalArgumentException("Width and height must be positive values");
        }
        
        try {
            // Read the original image
            ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData);
            BufferedImage originalImage = ImageIO.read(inputStream);
            
            if (originalImage == null) {
                throw new IllegalArgumentException("Invalid image format");
            }
            
            // Create a new BufferedImage with the specified width and height
            BufferedImage resizedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            
            // Use different scaling algorithms based on the size difference
            Object interpolationHint;
            double scaleRatio = Math.min(
                    (double) width / originalImage.getWidth(),
                    (double) height / originalImage.getHeight());
                    
            if (scaleRatio < 0.5) {
                // Significant downscaling - use multi-step scaling for better quality
                return multiStepResize(originalImage, width, height);
            } else {
                // Regular scaling
                Graphics2D g2d = resizedImage.createGraphics();
                
                try {
                    // Apply rendering hints for better quality
                    g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                    g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    
                    // Draw the original image to the new dimensions
                    g2d.drawImage(originalImage, 0, 0, width, height, null);
                } finally {
                    g2d.dispose();
                }
            }
            
            // Convert the resized image back to a byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            
            // Determine the format from the original image (fallback to JPEG)
            String formatName = "jpg";
            if (originalImage.getColorModel().hasAlpha()) {
                formatName = "png";
            }
            
            ImageIO.write(resizedImage, formatName, outputStream);
            
            return outputStream.toByteArray();
        } catch (IOException e) {
            logger.error("Failed to resize image", e);
            throw new RuntimeException("Failed to resize image", e);
        }
    }
    
    /**
     * Performs multi-step resizing for better quality when significant downscaling is needed
     * 
     * @param originalImage The original image
     * @param targetWidth Target width
     * @param targetHeight Target height
     * @return Resized image data
     * @throws IOException if image processing fails
     */
    private byte[] multiStepResize(BufferedImage originalImage, int targetWidth, int targetHeight) throws IOException {
        // Calculate the number of steps needed (logarithmic scaling works well)
        int steps = 2;
        double scaleFactor = Math.max(
                (double) targetWidth / originalImage.getWidth(),
                (double) targetHeight / originalImage.getHeight());
        
        if (scaleFactor < 0.3) {
            steps = 3;
        }
        if (scaleFactor < 0.1) {
            steps = 4;
        }
        
        BufferedImage currentImage = originalImage;
        
        // Perform step-by-step resizing
        for (int step = 0; step < steps; step++) {
            int stepWidth, stepHeight;
            
            if (step == steps - 1) {
                // Last step - use target dimensions
                stepWidth = targetWidth;
                stepHeight = targetHeight;
            } else {
                // Intermediate step - calculate intermediate dimensions
                double stepFactor = Math.pow(scaleFactor, (double) (step + 1) / steps);
                stepWidth = (int) (originalImage.getWidth() * stepFactor);
                stepHeight = (int) (originalImage.getHeight() * stepFactor);
            }
            
            // Create intermediate image
            BufferedImage stepImage = new BufferedImage(stepWidth, stepHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = stepImage.createGraphics();
            
            try {
                // Apply rendering hints
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2d.drawImage(currentImage, 0, 0, stepWidth, stepHeight, null);
            } finally {
                g2d.dispose();
            }
            
            // Set up for next iteration
            currentImage = stepImage;
        }
        
        // Convert final image to bytes
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(currentImage, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    /**
     * Generate thumbnail from original image
     * 
     * @param originalImageUrl URL of the original image
     * @return URL of the generated thumbnail
     * @throws IllegalArgumentException if original image URL is invalid
     * @throws RuntimeException if thumbnail generation fails
     */
    public String generateThumbnail(String originalImageUrl) {
        if (originalImageUrl == null || originalImageUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Original image URL cannot be null or empty");
        }
        
        try {
            // Extract filename from the URL
            String filename = originalImageUrl.substring(originalImageUrl.lastIndexOf('/') + 1);
            String fileExtension = filename.substring(filename.lastIndexOf('.'));
            String baseFilename = filename.substring(0, filename.lastIndexOf('.'));
            String thumbnailFilename = THUMBNAIL_PREFIX + baseFilename + fileExtension;
            
            // Get the original file
            Path originalFilePath = Paths.get(imageStoragePath, filename);
            Path thumbnailFilePath = Paths.get(imageStoragePath, thumbnailFilename);
            
            // Check if original image exists
            if (!Files.exists(originalFilePath)) {
                logger.error("Original image not found: {}", originalFilePath);
                throw new IllegalArgumentException("Original image not found: " + originalImageUrl);
            }
            
            // Read image data
            byte[] imageData = Files.readAllBytes(originalFilePath);
            
            // Resize to thumbnail size
            byte[] thumbnailData = resizeImage(imageData, THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT);
            
            // Save thumbnail
            Files.write(thumbnailFilePath, thumbnailData);
            
            logger.info("Generated thumbnail for image: {}", filename);
            
            // Return URL to the thumbnail
            return "/images/" + thumbnailFilename;
        } catch (IOException e) {
            logger.error("Failed to generate thumbnail for image: {}", originalImageUrl, e);
            throw new RuntimeException("Failed to generate thumbnail", e);
        }
    }

    /**
     * Validate image format and size
     * 
     * @param imageFile The image file to validate
     * @return true if image is valid, false otherwise
     */
    public Boolean validateImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) {
            logger.error("Image file is null or empty");
            return false;
        }
        
        // Check file size
        if (imageFile.getSize() > MAX_IMAGE_SIZE) {
            logger.error("Image size exceeds maximum allowed size of 5MB: {} bytes", imageFile.getSize());
            return false;
        }
        
        // Check content type
        String contentType = imageFile.getContentType();
        if (contentType == null || !allowedImageTypes.contains(contentType.toLowerCase())) {
            logger.error("Image type not allowed: {}", contentType);
            return false;
        }
        
        try {
            // Try to read the image to confirm it's a valid image file
            BufferedImage img = ImageIO.read(imageFile.getInputStream());
            if (img == null) {
                logger.error("Invalid image format for file: {}", imageFile.getOriginalFilename());
                return false;
            }
            
            // Additional validations for security
            if (img.getWidth() <= 0 || img.getHeight() <= 0) {
                logger.error("Invalid image dimensions: {}x{}", img.getWidth(), img.getHeight());
                return false;
            }
            
            // Check if the actual content matches the declared content type
            boolean isPng = contentType.equals("image/png") && hasAlphaChannel(img);
            boolean isJpeg = contentType.equals("image/jpeg") && !hasAlphaChannel(img);
            boolean isGif = contentType.equals("image/gif"); // Simplified check for GIF
            
            if ((contentType.equals("image/png") && !isPng) || 
                (contentType.equals("image/jpeg") && !isJpeg)) {
                logger.warn("Content type mismatch for file: {}", imageFile.getOriginalFilename());
                // Allow it anyway but log the warning
            }
            
            return true;
        } catch (IOException e) {
            logger.error("Error validating image: {}", imageFile.getOriginalFilename(), e);
            return false;
        } catch (Exception e) {
            logger.error("Unexpected error validating image: {}", imageFile.getOriginalFilename(), e);
            return false;
        }
    }
    
    /**
     * Check if an image has an alpha channel (for format validation)
     */
    private boolean hasAlphaChannel(BufferedImage image) {
        return image.getColorModel().hasAlpha();
    }

    /**
     * Set image as primary for product
     * 
     * @param productId ID of the product
     * @param imageId ID of the image to set as primary
     * @throws IllegalArgumentException if product or image not found
     */
    public void setPrimaryImage(Long productId, Long imageId) {
        // Validate inputs
        if (productId == null) {
            throw new IllegalArgumentException("Product ID cannot be null");
        }
        
        if (imageId == null) {
            throw new IllegalArgumentException("Image ID cannot be null");
        }
        
        // Check if product exists
        if (!productRepository.existsById(productId)) {
            logger.error("Product not found for ID: {}", productId);
            throw new IllegalArgumentException("Product not found for ID: " + productId);
        }
        
        // FIXME: Implement proper image repository and entity
        // Check if image exists and belongs to the product
        boolean imageExists = false; // This would be a database query
        boolean imageLinkedToProduct = false; // This would be a database query
        
        if (!imageExists) {
            logger.error("Image not found for ID: {}", imageId);
            throw new IllegalArgumentException("Image not found for ID: " + imageId);
        }
        
        if (!imageLinkedToProduct) {
            logger.error("Image {} does not belong to product {}", imageId, productId);
            throw new IllegalArgumentException("Image does not belong to the specified product");
        }
        
        try {
            // In a real implementation, we would:
            // 1. Update the product entity to set this image as primary
            // 2. Clear primary flag from any other images if needed
            // 3. Save changes to the database
            
            // Mock database update
            logger.info("Set image {} as primary for product {}", imageId, productId);
            
            // Update search index (for high complexity)
            updateSearchIndex(productId);
            
            // Update cache (for high complexity)
            invalidateProductCache(productId);
            
        } catch (Exception e) {
            logger.error("Failed to set primary image for product {}", productId, e);
            throw new RuntimeException("Failed to set primary image", e);
        }
    }
    
    /**
     * Update search index after product image changes
     * 
     * @param productId ID of the product to update in search index
     */
    private void updateSearchIndex(Long productId) {
        // Mock implementation - would be replaced with actual search index update
        try {
            // Simulate a complex operation that might fail
            if (Math.random() < 0.05) {
                throw new IOException("Random search index update failure");
            }
            
            // Simulate latency
            Thread.sleep((long) (Math.random() * 100));
            
            logger.debug("Updated search index for product: {}", productId);
        } catch (Exception e) {
            // Non-critical error - log but don't throw
            logger.warn("Failed to update search index for product {}", productId, e);
        }
    }
    
    /**
     * Invalidate product cache after image changes
     * 
     * @param productId ID of the product to invalidate in cache
     */
    private void invalidateProductCache(Long productId) {
        // Mock implementation - would be replaced with actual cache invalidation
        try {
            // Simulate different cache layers for high complexity
            String[] cacheLayers = {"local", "distributed", "cdn"};
            
            for (String layer : cacheLayers) {
                // Simulate different failure rates for different cache layers
                double failureRate = layer.equals("cdn") ? 0.1 : 0.01;
                
                if (Math.random() < failureRate) {
                    throw new IOException("Failed to invalidate " + layer + " cache");
                }
                
                // Simulate different latencies for different cache layers
                long latency = layer.equals("cdn") ? 200 : 50;
                Thread.sleep(latency);
                
                logger.debug("Invalidated {} cache for product: {}", layer, productId);
            }
        } catch (Exception e) {
            // Non-critical error - log but don't throw
            logger.warn("Failed to invalidate cache for product {}", productId, e);
        }
    }
}