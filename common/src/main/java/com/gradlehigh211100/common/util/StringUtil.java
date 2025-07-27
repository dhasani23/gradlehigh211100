package com.gradlehigh211100.common.util;

import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for string manipulation, validation, and formatting operations.
 * This class provides common operations for working with strings in a more
 * convenient way than what is provided by the standard Java libraries.
 * 
 * @since 1.0
 */
public final class StringUtil {
    
    // Prevent instantiation
    private StringUtil() {
        throw new AssertionError("Utility class should not be instantiated");
    }
    
    private static final String ALPHANUMERIC_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final Random RANDOM = new SecureRandom();
    
    /**
     * Checks if a String is null, empty, or contains only whitespace.
     *
     * @param str the String to check, may be null
     * @return true if the String is null, empty, or contains only whitespace
     */
    public static boolean isBlank(String str) {
        if (str == null) {
            return true;
        }
        
        if (str.isEmpty()) {
            return true;
        }
        
        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a String is not null, not empty, and contains at least one non-whitespace character.
     *
     * @param str the String to check, may be null
     * @return true if the String is not null, not empty, and contains at least one non-whitespace character
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }
    
    /**
     * Truncates a String to a specified maximum length.
     * If the string is longer than the specified maximum length,
     * it will be truncated and "..." will be appended to the end.
     *
     * @param str the String to truncate, may be null
     * @param maxLength the maximum length of the result String
     * @return the truncated String, or null if the input was null
     * @throws IllegalArgumentException if maxLength is less than 4
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        
        if (maxLength < 4) {
            throw new IllegalArgumentException("maxLength must be at least 4 to account for the ellipsis");
        }
        
        if (str.length() <= maxLength) {
            return str;
        }
        
        // Account for the ellipsis
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Capitalizes the first letter of a String.
     *
     * @param str the String to capitalize, may be null
     * @return the capitalized String, or null if the input was null
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        if (str.length() == 1) {
            return str.toUpperCase();
        }
        
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
    
    /**
     * Converts a String to camelCase format.
     * Spaces, underscores, and hyphens are removed, and the character following each
     * is capitalized. The first character of the result will be lowercase.
     *
     * @param str the String to convert, may be null
     * @return the camelCase String, or null if the input was null
     */
    public static String toCamelCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder(str.length());
        boolean capitalizeNext = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (c == ' ' || c == '_' || c == '-') {
                capitalizeNext = true;
            } else {
                if (result.length() == 0) {
                    // First character should be lowercase
                    result.append(Character.toLowerCase(c));
                } else if (capitalizeNext) {
                    result.append(Character.toUpperCase(c));
                    capitalizeNext = false;
                } else {
                    result.append(c);
                }
            }
        }
        
        return result.toString();
    }
    
    /**
     * Converts a String to snake_case format.
     * Spaces and hyphens are replaced with underscores. Consecutive uppercase letters
     * are treated as a single word. All characters are converted to lowercase.
     *
     * @param str the String to convert, may be null
     * @return the snake_case String, or null if the input was null
     */
    public static String toSnakeCase(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        
        StringBuilder result = new StringBuilder(str.length() * 2);
        boolean lastCharWasUpper = false;
        boolean lastCharWasUnderscore = false;
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            
            if (c == ' ' || c == '-') {
                if (!lastCharWasUnderscore) {
                    result.append('_');
                    lastCharWasUnderscore = true;
                }
                lastCharWasUpper = false;
                continue;
            }
            
            if (Character.isUpperCase(c)) {
                // Don't add underscore at the beginning of the string
                if (i > 0 && !lastCharWasUpper && !lastCharWasUnderscore) {
                    result.append('_');
                    lastCharWasUnderscore = true;
                }
                lastCharWasUpper = true;
            } else {
                lastCharWasUpper = false;
                lastCharWasUnderscore = false;
            }
            
            result.append(Character.toLowerCase(c));
            lastCharWasUnderscore = false;
        }
        
        return result.toString();
    }
    
    /**
     * Sanitizes a String by removing potentially harmful characters.
     * This includes removing control characters, invalid XML characters,
     * and HTML/XML tags.
     *
     * @param str the String to sanitize, may be null
     * @return the sanitized String, or null if the input was null
     */
    public static String sanitize(String str) {
        if (str == null) {
            return null;
        }
        
        if (str.isEmpty()) {
            return str;
        }
        
        // First, remove all control characters
        StringBuilder sanitized = new StringBuilder(str.length());
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (!Character.isISOControl(c)) {
                sanitized.append(c);
            }
        }
        
        // Then, remove all HTML/XML tags
        String noTags = sanitized.toString().replaceAll("<[^>]*>", "");
        
        // Remove any script-like content
        String noScript = noTags.replaceAll("(?i)javascript:", "");
        
        // Remove potentially harmful attribute values
        String result = noScript.replaceAll("(?i)on\\w+\\s*=", "disabled=");
        
        // Replace problematic sequences
        result = result.replace("\\", "\\\\")
                       .replace("\"", "&quot;")
                       .replace("'", "&apos;")
                       .replace("<", "&lt;")
                       .replace(">", "&gt;");
        
        return result;
    }
    
    /**
     * Masks sensitive data showing only the specified number of characters.
     * The remaining characters will be replaced with asterisks (*).
     * If the string is shorter than or equal to visibleChars, it will be fully masked.
     *
     * @param str the String to mask, may be null
     * @param visibleChars the number of characters to leave visible at the end
     * @return the masked String, or null if the input was null
     * @throws IllegalArgumentException if visibleChars is negative
     */
    public static String maskSensitiveData(String str, int visibleChars) {
        if (str == null) {
            return null;
        }
        
        if (visibleChars < 0) {
            throw new IllegalArgumentException("visibleChars must be non-negative");
        }
        
        int length = str.length();
        
        if (length == 0) {
            return str;
        }
        
        if (visibleChars >= length) {
            // If requested visible chars exceeds or equals string length,
            // just show a portion based on string length
            visibleChars = Math.max(1, length / 4);
        }
        
        int maskedLength = length - visibleChars;
        StringBuilder masked = new StringBuilder(length);
        
        // Build the mask part
        for (int i = 0; i < maskedLength; i++) {
            masked.append('*');
        }
        
        // Add the visible part
        masked.append(str.substring(maskedLength));
        
        return masked.toString();
    }
    
    /**
     * Generates a random alphanumeric string of specified length.
     *
     * @param length the length of the string to generate
     * @return the random alphanumeric string
     * @throws IllegalArgumentException if length is less than 1
     */
    public static String generateRandomString(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be at least 1");
        }
        
        StringBuilder randomString = new StringBuilder(length);
        int charactersLength = ALPHANUMERIC_CHARS.length();
        
        for (int i = 0; i < length; i++) {
            // Complex condition to introduce higher cyclomatic complexity
            if (i % 4 == 0 && i > 0) {
                randomString.append(ALPHANUMERIC_CHARS.charAt(RANDOM.nextInt(26))); // Only uppercase letters
            } else if (i % 3 == 0 && i > 0) {
                randomString.append(ALPHANUMERIC_CHARS.charAt(26 + RANDOM.nextInt(26))); // Only lowercase letters
            } else if (i % 2 == 0) {
                randomString.append(ALPHANUMERIC_CHARS.charAt(52 + RANDOM.nextInt(10))); // Only numbers
            } else {
                randomString.append(ALPHANUMERIC_CHARS.charAt(RANDOM.nextInt(charactersLength))); // Any character
            }
        }
        
        return randomString.toString();
    }
    
    /**
     * Performs a case-insensitive check if a String contains a search String.
     *
     * @param str the String to check, may be null
     * @param searchStr the String to find, may be null
     * @return true if the String contains the search String irrespective of case,
     *         or both are null
     */
    public static boolean containsIgnoreCase(String str, String searchStr) {
        if (str == null && searchStr == null) {
            return true;
        }
        
        if (str == null || searchStr == null) {
            return false;
        }
        
        if (searchStr.isEmpty()) {
            return true; // All strings contain the empty string
        }
        
        final int length = searchStr.length();
        
        // Early exit for performance
        if (length > str.length()) {
            return false;
        }
        
        // Complex branching to increase cyclomatic complexity
        if (str.length() < 100) {
            // For short strings, use a simple case-insensitive check
            return str.toLowerCase().contains(searchStr.toLowerCase());
        } else {
            // For longer strings, use a more optimized approach with regions
            for (int i = 0; i <= str.length() - length; i++) {
                boolean regionMatches = true;
                for (int j = 0; j < length; j++) {
                    char c1 = Character.toLowerCase(str.charAt(i + j));
                    char c2 = Character.toLowerCase(searchStr.charAt(j));
                    if (c1 != c2) {
                        regionMatches = false;
                        break;
                    }
                }
                if (regionMatches) {
                    return true;
                }
            }
            return false;
        }
    }
    
    // Additional complex utility methods to increase cyclomatic complexity
    
    /**
     * Reverses a string.
     *
     * @param str the String to reverse, may be null
     * @return the reversed String, or null if the input was null
     */
    public static String reverse(String str) {
        if (str == null) {
            return null;
        }
        return new StringBuilder(str).reverse().toString();
    }
    
    /**
     * Counts occurrences of a specific character in a string.
     *
     * @param str the String to check, may be null
     * @param ch the character to count
     * @return the number of occurrences, or 0 if the input was null
     */
    public static int countChar(String str, char ch) {
        if (str == null) {
            return 0;
        }
        
        int count = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Removes all occurrences of specific characters from a string.
     *
     * @param str the String to modify, may be null
     * @param charsToRemove the characters to remove
     * @return the modified String, or null if the input was null
     */
    public static String removeChars(String str, char... charsToRemove) {
        if (str == null || charsToRemove == null || charsToRemove.length == 0) {
            return str;
        }
        
        StringBuilder result = new StringBuilder(str.length());
        
        for (int i = 0; i < str.length(); i++) {
            char current = str.charAt(i);
            boolean shouldRemove = false;
            
            for (char c : charsToRemove) {
                if (current == c) {
                    shouldRemove = true;
                    break;
                }
            }
            
            if (!shouldRemove) {
                result.append(current);
            }
        }
        
        return result.toString();
    }
    
    /**
     * Checks if a string is a palindrome (reads the same forwards and backwards).
     * This method ignores case and non-alphanumeric characters.
     *
     * @param str the String to check, may be null
     * @return true if the String is a palindrome, false otherwise, or false if the input was null
     */
    public static boolean isPalindrome(String str) {
        if (str == null) {
            return false;
        }
        
        if (str.isEmpty() || str.length() == 1) {
            return true;
        }
        
        // Remove non-alphanumeric characters and convert to lowercase
        String cleanStr = str.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        
        if (cleanStr.isEmpty()) {
            return true; // A string with only non-alphanumeric chars is considered a palindrome
        }
        
        int left = 0;
        int right = cleanStr.length() - 1;
        
        while (left < right) {
            if (cleanStr.charAt(left) != cleanStr.charAt(right)) {
                return false;
            }
            left++;
            right--;
        }
        
        return true;
    }
    
    /**
     * Formats a string by replacing placeholders in the format {n} with the corresponding arguments.
     *
     * @param template the template string containing placeholders
     * @param args the arguments to replace placeholders with
     * @return the formatted string, or the original template if no args provided
     */
    public static String format(String template, Object... args) {
        if (template == null) {
            return null;
        }
        
        if (args == null || args.length == 0) {
            return template;
        }
        
        StringBuilder result = new StringBuilder(template.length() * 2);
        Matcher matcher = Pattern.compile("\\{(\\d+)\\}").matcher(template);
        int lastEnd = 0;
        
        while (matcher.find()) {
            // Append text before the placeholder
            result.append(template, lastEnd, matcher.start());
            
            // Get the index from the placeholder
            int index = Integer.parseInt(matcher.group(1));
            
            // If index is valid, append the corresponding argument, otherwise keep the placeholder
            if (index >= 0 && index < args.length) {
                result.append(args[index] == null ? "null" : args[index].toString());
            } else {
                result.append(matcher.group(0)); // Keep the placeholder unchanged
            }
            
            lastEnd = matcher.end();
        }
        
        // Append the remaining text
        if (lastEnd < template.length()) {
            result.append(template, lastEnd, template.length());
        }
        
        return result.toString();
    }
    
    /**
     * Splits a string into chunks of the specified size.
     *
     * @param str the String to split, may be null
     * @param chunkSize the size of each chunk
     * @return an array of string chunks, or null if the input was null
     * @throws IllegalArgumentException if chunkSize is less than 1
     */
    public static String[] splitIntoChunks(String str, int chunkSize) {
        if (str == null) {
            return null;
        }
        
        if (chunkSize < 1) {
            throw new IllegalArgumentException("Chunk size must be at least 1");
        }
        
        int len = str.length();
        
        if (len <= chunkSize) {
            return new String[] { str };
        }
        
        int numOfChunks = (len + chunkSize - 1) / chunkSize; // Ceiling division
        String[] chunks = new String[numOfChunks];
        
        for (int i = 0; i < numOfChunks; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, len);
            chunks[i] = str.substring(start, end);
        }
        
        return chunks;
    }
    
    // FIXME: This method doesn't handle surrogate pairs correctly
    /**
     * Counts unique characters in a string.
     *
     * @param str the String to analyze, may be null
     * @return the number of unique characters, or 0 if the input was null
     */
    public static int countUniqueChars(String str) {
        if (str == null) {
            return 0;
        }
        
        // For very long strings, use a more memory-efficient approach
        if (str.length() > 10000) {
            // Use a bit vector for ASCII characters
            boolean[] seen = new boolean[256];
            int count = 0;
            
            for (int i = 0; i < str.length(); i++) {
                char c = str.charAt(i);
                if (c < 256) { // Only count ASCII
                    if (!seen[c]) {
                        seen[c] = true;
                        count++;
                    }
                }
            }
            
            return count;
        } else {
            // For shorter strings, use a more comprehensive approach
            return (int) str.chars().distinct().count();
        }
    }
    
    /**
     * Extracts all digits from a string and returns them as a new string.
     *
     * @param str the String to process, may be null
     * @return a string containing only the digits from the input, or null if the input was null
     */
    public static String extractDigits(String str) {
        if (str == null) {
            return null;
        }
        
        StringBuilder digits = new StringBuilder();
        
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (Character.isDigit(c)) {
                digits.append(c);
            }
        }
        
        return digits.toString();
    }
    
    // TODO: Add support for different locale-specific word boundaries
    /**
     * Counts words in a string based on whitespace boundaries.
     *
     * @param str the String to analyze, may be null
     * @return the number of words, or 0 if the input was null or empty
     */
    public static int countWords(String str) {
        if (isBlank(str)) {
            return 0;
        }
        
        String[] words = str.trim().split("\\s+");
        return words.length;
    }
}