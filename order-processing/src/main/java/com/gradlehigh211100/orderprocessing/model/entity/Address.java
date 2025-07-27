package com.gradlehigh211100.orderprocessing.model.entity;

import javax.persistence.Column;
import javax.persistence.Embeddable;
import java.util.Objects;

/**
 * Embeddable entity representing a physical address.
 * Used for shipping and billing addresses in orders.
 */
@Embeddable
public class Address {

    @Column(name = "street")
    private String street;
    
    @Column(name = "city")
    private String city;
    
    @Column(name = "state")
    private String state;
    
    @Column(name = "zipcode")
    private String zipCode;
    
    @Column(name = "country")
    private String country;
    
    @Column(name = "region")
    private String region;
    
    /**
     * Default constructor required by JPA
     */
    public Address() {
        // Required by JPA
    }
    
    /**
     * Creates a new address with the specified details
     * 
     * @param street the street address including house/building number
     * @param city the city
     * @param state the state or province
     * @param zipCode the postal/zip code
     * @param country the country
     */
    public Address(String street, String city, String state, String zipCode, String country) {
        this.street = street;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.country = country;
        
        // Determine region based on country
        if ("US".equalsIgnoreCase(country) || "Canada".equalsIgnoreCase(country)) {
            this.region = "NORTH_AMERICA";
        } else if (isEuropeanCountry(country)) {
            this.region = "EUROPE";
        } else {
            this.region = "INTERNATIONAL";
        }
    }
    
    /**
     * Helper method to determine if a country is in Europe
     * 
     * @param country the country to check
     * @return true if the country is in Europe
     */
    private boolean isEuropeanCountry(String country) {
        // Simplified implementation - in a real system this would be more comprehensive
        String[] europeanCountries = {
            "Austria", "Belgium", "Bulgaria", "Croatia", "Cyprus", "Czech Republic",
            "Denmark", "Estonia", "Finland", "France", "Germany", "Greece", "Hungary",
            "Ireland", "Italy", "Latvia", "Lithuania", "Luxembourg", "Malta", "Netherlands",
            "Poland", "Portugal", "Romania", "Slovakia", "Slovenia", "Spain", "Sweden", "UK"
        };
        
        for (String europeanCountry : europeanCountries) {
            if (europeanCountry.equalsIgnoreCase(country)) {
                return true;
            }
        }
        
        return false;
    }
    
    // Getters and Setters
    
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
    
    public String getRegion() {
        return region;
    }
    
    public void setRegion(String region) {
        this.region = region;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        
        Address address = (Address) o;
        
        return Objects.equals(street, address.street) &&
               Objects.equals(city, address.city) &&
               Objects.equals(state, address.state) &&
               Objects.equals(zipCode, address.zipCode) &&
               Objects.equals(country, address.country);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(street, city, state, zipCode, country);
    }
    
    @Override
    public String toString() {
        return street + ", " + city + ", " + state + " " + zipCode + ", " + country;
    }
}