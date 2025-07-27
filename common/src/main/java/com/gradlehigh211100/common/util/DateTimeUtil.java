package com.gradlehigh211100.common.util;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class providing comprehensive date and time manipulation operations.
 * This class contains methods for formatting, parsing, and manipulating date and time objects
 * in various ways, supporting different time zones and formatting patterns.
 */
public final class DateTimeUtil {

    private static final String DEFAULT_DATE_PATTERN = "yyyy-MM-dd";
    private static final String DEFAULT_DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    
    private static final Set<DayOfWeek> WEEKEND_DAYS = EnumSet.of(
            DayOfWeek.SATURDAY, 
            DayOfWeek.SUNDAY
    );
    
    /**
     * Private constructor to prevent instantiation of utility class.
     */
    private DateTimeUtil() {
        throw new AssertionError("DateTimeUtil class should not be instantiated");
    }

    /**
     * Returns the current timestamp in the system timezone.
     *
     * @return current date and time
     */
    public static LocalDateTime getCurrentTimestamp() {
        return LocalDateTime.now();
    }

    /**
     * Returns the current timestamp in the specified timezone.
     *
     * @param zoneId the timezone ID
     * @return current date and time in specified timezone
     * @throws IllegalArgumentException if zoneId is invalid
     */
    public static LocalDateTime getCurrentTimestamp(String zoneId) {
        if (zoneId == null || zoneId.trim().isEmpty()) {
            throw new IllegalArgumentException("Zone ID cannot be null or empty");
        }

        try {
            ZoneId zone = ZoneId.of(zoneId);
            return LocalDateTime.now(zone);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid zone ID: " + zoneId, e);
        }
    }

    /**
     * Formats a LocalDate object according to the specified pattern.
     *
     * @param date the date to format
     * @param pattern the formatting pattern
     * @return formatted date string
     * @throws IllegalArgumentException if date is null or pattern is invalid
     */
    public static String formatDate(LocalDate date, String pattern) {
        validateNotNull(date, "Date cannot be null");
        
        String actualPattern = pattern;
        if (actualPattern == null || actualPattern.trim().isEmpty()) {
            actualPattern = DEFAULT_DATE_PATTERN;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(actualPattern);
            return date.format(formatter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date pattern: " + pattern, e);
        }
    }

    /**
     * Formats a LocalDateTime object according to the specified pattern.
     *
     * @param dateTime the date-time to format
     * @param pattern the formatting pattern
     * @return formatted date-time string
     * @throws IllegalArgumentException if dateTime is null or pattern is invalid
     */
    public static String formatDateTime(LocalDateTime dateTime, String pattern) {
        validateNotNull(dateTime, "DateTime cannot be null");
        
        String actualPattern = pattern;
        if (actualPattern == null || actualPattern.trim().isEmpty()) {
            actualPattern = DEFAULT_DATE_TIME_PATTERN;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(actualPattern);
            return dateTime.format(formatter);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date-time pattern: " + pattern, e);
        }
    }

    /**
     * Parses a string into a LocalDate using the specified pattern.
     *
     * @param dateString the string to parse
     * @param pattern the pattern to use for parsing
     * @return the parsed LocalDate object
     * @throws IllegalArgumentException if dateString is null or pattern is invalid
     * @throws DateTimeParseException if dateString cannot be parsed
     */
    public static LocalDate parseDate(String dateString, String pattern) {
        validateNotNull(dateString, "Date string cannot be null");
        
        String actualPattern = pattern;
        if (actualPattern == null || actualPattern.trim().isEmpty()) {
            actualPattern = DEFAULT_DATE_PATTERN;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(actualPattern);
            return LocalDate.parse(dateString, formatter);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Failed to parse date: " + dateString, dateString, e.getErrorIndex());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date pattern: " + pattern, e);
        }
    }

    /**
     * Parses a string into a LocalDateTime using the specified pattern.
     *
     * @param dateTimeString the string to parse
     * @param pattern the pattern to use for parsing
     * @return the parsed LocalDateTime object
     * @throws IllegalArgumentException if dateTimeString is null or pattern is invalid
     * @throws DateTimeParseException if dateTimeString cannot be parsed
     */
    public static LocalDateTime parseDateTime(String dateTimeString, String pattern) {
        validateNotNull(dateTimeString, "DateTime string cannot be null");
        
        String actualPattern = pattern;
        if (actualPattern == null || actualPattern.trim().isEmpty()) {
            actualPattern = DEFAULT_DATE_TIME_PATTERN;
        }
        
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(actualPattern);
            return LocalDateTime.parse(dateTimeString, formatter);
        } catch (DateTimeParseException e) {
            throw new DateTimeParseException("Failed to parse date-time: " + dateTimeString, 
                    dateTimeString, e.getErrorIndex());
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid date-time pattern: " + pattern, e);
        }
    }

    /**
     * Adds the specified number of days to the given date.
     *
     * @param date the base date
     * @param days the number of days to add (can be negative)
     * @return a new date with days added
     * @throws IllegalArgumentException if date is null
     */
    public static LocalDate addDays(LocalDate date, long days) {
        validateNotNull(date, "Date cannot be null");
        return date.plusDays(days);
    }

    /**
     * Calculates the number of days between two dates.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return the number of days between startDate and endDate
     * @throws IllegalArgumentException if either date is null
     */
    public static long daysBetween(LocalDate startDate, LocalDate endDate) {
        validateNotNull(startDate, "Start date cannot be null");
        validateNotNull(endDate, "End date cannot be null");
        
        return ChronoUnit.DAYS.between(startDate, endDate);
    }

    /**
     * Checks if the given date falls on a weekend (Saturday or Sunday).
     *
     * @param date the date to check
     * @return true if the date is a weekend, false otherwise
     * @throws IllegalArgumentException if date is null
     */
    public static boolean isWeekend(LocalDate date) {
        validateNotNull(date, "Date cannot be null");
        return WEEKEND_DAYS.contains(date.getDayOfWeek());
    }

    /**
     * Returns the start of day (00:00:00) for the given date.
     *
     * @param date the date
     * @return LocalDateTime representing start of the given date
     * @throws IllegalArgumentException if date is null
     */
    public static LocalDateTime getStartOfDay(LocalDate date) {
        validateNotNull(date, "Date cannot be null");
        return date.atStartOfDay();
    }

    /**
     * Returns the end of day (23:59:59.999999999) for the given date.
     *
     * @param date the date
     * @return LocalDateTime representing end of the given date
     * @throws IllegalArgumentException if date is null
     */
    public static LocalDateTime getEndOfDay(LocalDate date) {
        validateNotNull(date, "Date cannot be null");
        return LocalDateTime.of(date, LocalTime.MAX);
    }
    
    /**
     * Checks if the given date is in the future.
     *
     * @param date the date to check
     * @return true if the date is in the future, false otherwise
     * @throws IllegalArgumentException if date is null
     */
    public static boolean isFutureDate(LocalDate date) {
        validateNotNull(date, "Date cannot be null");
        return date.isAfter(LocalDate.now());
    }
    
    /**
     * Generates a list of dates between start date (inclusive) and end date (inclusive).
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of dates between startDate and endDate
     * @throws IllegalArgumentException if either date is null or if endDate is before startDate
     */
    public static List<LocalDate> getDatesBetween(LocalDate startDate, LocalDate endDate) {
        validateNotNull(startDate, "Start date cannot be null");
        validateNotNull(endDate, "End date cannot be null");
        
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date");
        }
        
        List<LocalDate> dates = new ArrayList<>();
        LocalDate currentDate = startDate;
        
        while (!currentDate.isAfter(endDate)) {
            dates.add(currentDate);
            currentDate = currentDate.plusDays(1);
        }
        
        return dates;
    }
    
    /**
     * Calculates age in years from birth date to current date.
     *
     * @param birthDate the birth date
     * @return age in years
     * @throws IllegalArgumentException if birthDate is null or in the future
     */
    public static int calculateAge(LocalDate birthDate) {
        validateNotNull(birthDate, "Birth date cannot be null");
        
        LocalDate currentDate = LocalDate.now();
        
        if (birthDate.isAfter(currentDate)) {
            throw new IllegalArgumentException("Birth date cannot be in the future");
        }
        
        Period period = Period.between(birthDate, currentDate);
        return period.getYears();
    }
    
    /**
     * Converts LocalDateTime to milliseconds since epoch.
     *
     * @param dateTime the LocalDateTime to convert
     * @return milliseconds since epoch
     * @throws IllegalArgumentException if dateTime is null
     */
    public static long toEpochMilli(LocalDateTime dateTime) {
        validateNotNull(dateTime, "DateTime cannot be null");
        
        ZonedDateTime zonedDateTime = dateTime.atZone(ZoneId.systemDefault());
        return zonedDateTime.toInstant().toEpochMilli();
    }
    
    /**
     * Creates LocalDateTime from milliseconds since epoch.
     *
     * @param epochMilli milliseconds since epoch
     * @return LocalDateTime
     */
    public static LocalDateTime fromEpochMilli(long epochMilli) {
        return LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(epochMilli),
                ZoneId.systemDefault()
        );
    }

    /**
     * Calculates the difference between two LocalDateTime objects in terms of hours, minutes, and seconds.
     *
     * @param startDateTime the start date and time
     * @param endDateTime the end date and time
     * @return formatted string showing the difference
     * @throws IllegalArgumentException if either parameter is null
     */
    public static String getTimeDifference(LocalDateTime startDateTime, LocalDateTime endDateTime) {
        validateNotNull(startDateTime, "Start datetime cannot be null");
        validateNotNull(endDateTime, "End datetime cannot be null");
        
        Duration duration = Duration.between(startDateTime, endDateTime);
        
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        long seconds = duration.toSecondsPart();
        
        StringBuilder result = new StringBuilder();
        
        if (hours > 0) {
            result.append(hours).append(" hour");
            if (hours != 1) result.append("s");
            
            if (minutes > 0 || seconds > 0) result.append(", ");
        }
        
        if (minutes > 0) {
            result.append(minutes).append(" minute");
            if (minutes != 1) result.append("s");
            
            if (seconds > 0) result.append(", ");
        }
        
        if (seconds > 0 || (hours == 0 && minutes == 0)) {
            result.append(seconds).append(" second");
            if (seconds != 1) result.append("s");
        }
        
        return result.toString();
    }
    
    /**
     * Truncates the time portion of a LocalDateTime to the specified unit.
     * 
     * @param dateTime the date-time to truncate
     * @param unit the temporal unit to truncate to
     * @return the truncated LocalDateTime
     * @throws IllegalArgumentException if dateTime is null or unit is invalid
     */
    public static LocalDateTime truncateTo(LocalDateTime dateTime, ChronoUnit unit) {
        validateNotNull(dateTime, "DateTime cannot be null");
        validateNotNull(unit, "ChronoUnit cannot be null");
        
        // FIXME: This doesn't handle all ChronoUnit values correctly, needs refinement
        switch (unit) {
            case DAYS:
                return dateTime.truncatedTo(ChronoUnit.DAYS);
            case HOURS:
                return dateTime.truncatedTo(ChronoUnit.HOURS);
            case MINUTES:
                return dateTime.truncatedTo(ChronoUnit.MINUTES);
            case SECONDS:
                return dateTime.truncatedTo(ChronoUnit.SECONDS);
            case MILLIS:
                return dateTime.truncatedTo(ChronoUnit.MILLIS);
            default:
                throw new IllegalArgumentException("Unsupported unit for truncation: " + unit);
        }
    }
    
    /**
     * Gets the next working day (skipping weekends) from the given date.
     *
     * @param date the starting date
     * @return the next working day
     * @throws IllegalArgumentException if date is null
     */
    public static LocalDate getNextWorkingDay(LocalDate date) {
        validateNotNull(date, "Date cannot be null");
        
        LocalDate nextDay = date.plusDays(1);
        
        // Keep moving forward until we find a non-weekend day
        while (isWeekend(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        
        return nextDay;
    }
    
    /**
     * Helper method to validate that a parameter is not null.
     *
     * @param object the object to check
     * @param message the error message to use if object is null
     * @throws IllegalArgumentException if object is null
     */
    private static void validateNotNull(Object object, String message) {
        if (object == null) {
            throw new IllegalArgumentException(message);
        }
    }
    
    // TODO: Add support for different calendar systems
    // TODO: Add timezone conversion utilities
    // TODO: Add fiscal date/quarter utilities
}