package com.example.helloworld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloWorldApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }

}




import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Complete Location class containing all fields for both homeowner and commercial locations
 */
@Data
@Builder
public class Location {
    // Line identification fields
    private String lineName;
    private String lineNumber;
    private String lineNumberText;
    private String locLineNo;

    // Common location identification fields
    private String locName;
    private String locAddressType;
    private String locParcel;
    private String locLot;
    private String locBlock;
    private String locPlan;
    private String locQuarter;
    private String locSection;
    private String locTownship;
    private String locRange;
    private String locMeridian;

    // Address fields
    private String locCivSuiteNo;
    private String locCivStreetNo;
    private String locCivStreetName;
    private String locStreetCode;
    private String locStreetDirection;
    private String locLocationDesc;
    private String locCity;
    private String locProv;
    private String locPostCode;

    // Location indicators
    private String locNearInd;
    private String locNearLocName;
    private String locWithinLocName;

    // Business and coverage fields (common)
    private String locationBusCode;
    private String locBMCov;
    private String locBMChgCd;

    // Commercial-specific fields
    private Double locBuildingLimit;
    private Double locDeduct;
    private Double locContentsLimit;
    private Double locContentsDeduct;
    private Double polCONLimit;

    // Business interruption fields (commercial)
    private String locBIForm1;
    private Double locBILimit1;
    private String locBIForm2;
    private Double locBILimit2;
    private String locBIForm3;
    private Double locBILimit3;
    private String locBIForm4;
    private Double locBILimit4;
    private String locBIForm5;
    private Double locBILimit5;
    private String locBIForm6;
    private Double locBILimit6;

    // Homeowner-specific fields (HSP - Home Service Plan)
    private Double locHSPFTPrem;
    private Double locHSPPremWrit;
    private Double locHSPComm;
    private Double locHSPDeduct;
    private Double locHSPLimit;

    // Homeowner-specific fields (SLC - Service Line Coverage)
    private Double locSLCFTPrem;
    private Double locSLCPremWrit;
    private Double locSLCComm;
    private Double locSLCDeduct;
    private Double locSLCLimit;

    // Record type indicator
    private LocationType locationType;

    // Additional system fields
    private Integer ediRecNo;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private Integer createUser;
    private Integer updateUser;
    private String errorMessage;
    private Boolean isValid;

    // Helper methods for business logic
    public boolean isHomeowner() {
        return LocationType.HOMEOWNER.equals(this.locationType);
    }

    public boolean isCommercial() {
        return LocationType.COMMERCIAL.equals(this.locationType);
    }

    public Double getTotalLimit() {
        if (isHomeowner()) {
            return locHSPLimit != null ? locHSPLimit : 0.0;
        } else {
            return (locBuildingLimit != null ? locBuildingLimit : 0.0) +
                   (locContentsLimit != null ? locContentsLimit : 0.0);
        }
    }

    public String getFormattedAddress() {
        StringBuilder address = new StringBuilder();
        
        // Add suite number if exists
        if (StringUtils.isNotEmpty(locCivSuiteNo)) {
            address.append(locCivSuiteNo).append("-");
        }
        
        // Add street number and name
        if (StringUtils.isNotEmpty(locCivStreetNo)) {
            address.append(locCivStreetNo).append(" ");
        }
        if (StringUtils.isNotEmpty(locCivStreetName)) {
            address.append(locCivStreetName).append(" ");
        }
        if (StringUtils.isNotEmpty(locStreetDirection)) {
            address.append(locStreetDirection);
        }
        
        return address.toString().trim();
    }

    public String getFormattedCityProvince() {
        StringBuilder cityProv = new StringBuilder();
        
        if (StringUtils.isNotEmpty(locCity)) {
            cityProv.append(locCity);
        }
        if (StringUtils.isNotEmpty(locProv)) {
            if (cityProv.length() > 0) {
                cityProv.append(", ");
            }
            cityProv.append(locProv);
        }
        if (StringUtils.isNotEmpty(locPostCode)) {
            if (cityProv.length() > 0) {
                cityProv.append(" ");
            }
            cityProv.append(locPostCode);
        }
        
        return cityProv.toString();
    }

    public boolean hasValidAddress() {
        return StringUtils.isNotEmpty(locCivStreetNo) &&
               StringUtils.isNotEmpty(locCivStreetName) &&
               StringUtils.isNotEmpty(locCity) &&
               StringUtils.isNotEmpty(locProv) &&
               StringUtils.isNotEmpty(locPostCode);
    }

    public boolean hasValidLimits() {
        if (isHomeowner()) {
            return locHSPLimit != null || locSLCLimit != null;
        } else {
            return locBuildingLimit != null || locContentsLimit != null;
        }
    }
}

