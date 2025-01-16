package com.example.helloworld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloWorldApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }

}



@Slf4j
@Component
public class SGILocationService {

    /**
     * Creates a Location object from the fieldSet based on type
     */
    public Location createLocation(FieldSet fieldSet, LocationType type) {
        try {
            // Extract and clean first column (LocLineNo)
            String firstColumn = fieldSet.readString(LOCATION_RECORD.LocLineNo.name())
                .replace("[", StringUtils.EMPTY)
                .replace("]", StringUtils.EMPTY);

            // Initialize builder with common fields
            Location.LocationBuilder builder = Location.builder()
                // Line details
                .lineName(LineUtils.determineEdiLineName(firstColumn))
                .lineNumber(LineUtils.determineEdiLineNumber(firstColumn))
                .lineNumberText(LineUtils.determineEdiLineNumberText(firstColumn))
                .locLineNo(firstColumn)

                // Location identification
                .locName("Building #" + fieldSet.readString(LOCATION_RECORD.LocName.name()))
                .locAddressType(fieldSet.readString(LOCATION_RECORD.LocAddressType.name()))
                .locParcel(fieldSet.readString(LOCATION_RECORD.LocParcel.name()))
                .locLot(fieldSet.readString(LOCATION_RECORD.LocLot.name()))
                .locBlock(fieldSet.readString(LOCATION_RECORD.LocBlock.name()))
                .locPlan(fieldSet.readString(LOCATION_RECORD.LocPlan.name()))

                // Geographic details
                .locQuarter(fieldSet.readString(LOCATION_RECORD.LocQuarter.name()))
                .locSection(fieldSet.readString(LOCATION_RECORD.LocSection.name()))
                .locTownship(fieldSet.readString(LOCATION_RECORD.LocTownship.name()))
                .locRange(fieldSet.readString(LOCATION_RECORD.LocRange.name()))
                .locMeridian(fieldSet.readString(LOCATION_RECORD.LocMeridian.name()))

                // Address details
                .locCivSuiteNo(fieldSet.readString(LOCATION_RECORD.LocCivSuiteNo.name()))
                .locCivStreetNo(fieldSet.readString(LOCATION_RECORD.LocCivStreetNo.name()))
                .locCivStreetName(fieldSet.readString(LOCATION_RECORD.LocCivStreetName.name()))
                .locStreetCode(fieldSet.readString(LOCATION_RECORD.LocStreetCode.name()))
                .locStreetDirection(fieldSet.readString(LOCATION_RECORD.LocStreetDirection.name()))
                .locLocationDesc(fieldSet.readString(LOCATION_RECORD.LocLocationDesc.name()))

                // City and postal details
                .locCity(fieldSet.readString(LOCATION_RECORD.LocCity.name()))
                .locProv(fieldSet.readString(LOCATION_RECORD.LocProv.name()))
                .locPostCode(fieldSet.readString(LOCATION_RECORD.LocPostCode.name()))

                // Location indicators
                .locNearInd(fieldSet.readString(LOCATION_RECORD.LocNearInd.name()))
                .locNearLocName(fieldSet.readString(LOCATION_RECORD.LocNearLocName.name()))
                .locWithinLocName(fieldSet.readString(LOCATION_RECORD.LocWithinLocName.name()));

            // Add type-specific fields
            if (type == LocationType.HOMEOWNER) {
                populateHomeownerFields(builder, fieldSet);
            } else {
                populateCommercialFields(builder, fieldSet);
            }

            return builder.build();

        } catch (Exception e) {
            log.error("Error creating location: {}", e.getMessage());
            throw new LocationProcessingException("Failed to create location", e);
        }
    }

    /**
     * Populates homeowner-specific fields (33 total fields)
     */
    private void populateHomeownerFields(Location.LocationBuilder builder, FieldSet fieldSet) {
        builder
            // Business and coverage details
            .locationBusCode(SGIConstants.NUMBER_ONE)
            .locBMCov(SGIConstants.HOMEOWNERS)
            .locBMChgCd(fieldSet.readString(LOCATION_RECORD.LocBMChgCd.name()))
            
            // HSP (Home Service Plan) details
            .locHSPFTPrem(parseDouble(fieldSet.readString(LOCATION_RECORD.LocHSPFTPrem.name())))
            .locHSPPremWrit(parseDouble(fieldSet.readString(LOCATION_RECORD.LocHSPPremWrit.name())))
            .locHSPComm(parseDouble(fieldSet.readString(LOCATION_RECORD.LocHSPComm.name())))
            .locHSPDeduct(parseDouble(fieldSet.readString(LOCATION_RECORD.LocHSPDeduct.name())))
            
            // SLC (Service Line Coverage) details
            .locSLCFTPrem(parseDouble(fieldSet.readString(LOCATION_RECORD.LocSLCFTPrem.name())))
            .locSLCPremWrit(parseDouble(fieldSet.readString(LOCATION_RECORD.LocSLCPremWrit.name())))
            .locSLCComm(parseDouble(fieldSet.readString(LOCATION_RECORD.LocSLCComm.name())))
            .locSLCDeduct(parseDouble(fieldSet.readString(LOCATION_RECORD.LocSLCDeduct.name())));
    }

    /**
     * Populates commercial-specific fields (44 total fields)
     */
    private void populateCommercialFields(Location.LocationBuilder builder, FieldSet fieldSet) {
        builder
            // Business and coverage details
            .locationBusCode(fieldSet.readString(LOCATION_RECORD.LocationBusCode.name()))
            .locBMCov(fieldSet.readString(LOCATION_RECORD.LocBMCov.name()))
            .locBMChgCd(fieldSet.readString(LOCATION_RECORD.CommLocBMChgCd.name()))
            
            // Building and contents limits
            .locBuildingLimit(parseDouble(fieldSet.readString(LOCATION_RECORD.LocBuildingLimit.name())))
            .locDeduct(parseDouble(fieldSet.readString(LOCATION_RECORD.LocDeduct.name())))
            .locContentsLimit(parseDouble(fieldSet.readString(LOCATION_RECORD.LocContentsLimit.name())))
            .locContentsDeduct(parseDouble(fieldSet.readString(LOCATION_RECORD.LocContentsDeduct.name())))
            .polCONLimit(parseDouble(fieldSet.readString(LOCATION_RECORD.PolCONLimit.name())))
            
            // Business interruption forms and limits
            .locBIForm1(fieldSet.readString(LOCATION_RECORD.LocBIForm1.name()))
            .locBILimit1(parseDouble(fieldSet.readString(LOCATION_RECORD.LocBILimit1.name())))
            .locBIForm2(fieldSet.readString(LOCATION_RECORD.LocBIForm2.name()))
            .locBILimit2(parseDouble(fieldSet.readString(LOCATION_RECORD.LocBILimit2.name())))
            .locBIForm3(fieldSet.readString(LOCATION_RECORD.LocBIForm3.name()))
            .locBILimit3(parseDouble(fieldSet.readString(LOCATION_RECORD.LocBILimit3.name())))
            .locBIForm4(fieldSet.readString(LOCATION_RECORD.LocBIForm4.name()))
            .locBILimit4(parseDouble(fieldSet.readString(LOCATION_RECORD.LocBILimit4.name())))
            .locBIForm5(fieldSet.readString(LOCATION_RECORD.LocBIForm5.name()))
            .locBILimit5(parseDouble(fieldSet.readString(LOCATION_RECORD.LocBILimit5.name())))
            .locBIForm6(fieldSet.readString(LOCATION_RECORD.LocBIForm6.name()))
            .locBILimit6(parseDouble(fieldSet.readString(LOCATION_RECORD.LocBILimit6.name())));
    }

    /**
     * Safely parses double values, returning null for invalid input
     */
    private Double parseDouble(String value) {
        try {
            return StringUtils.isNotEmpty(value) && !value.equals("NULL") ? 
                   Double.valueOf(value) : null;
        } catch (NumberFormatException e) {
            log.warn("Failed to parse double value: {}", value);
            return null;
        }
    }
}

public enum LocationType {
    HOMEOWNER(33),
    COMMERCIAL(44);

    private final int fieldCount;

    LocationType(int fieldCount) {
        this.fieldCount = fieldCount;
    }

    public int getFieldCount() {
        return fieldCount;
    }
}


private LocationType determineLocationType(FieldSet fieldSet) {
        // Implement your logic to determine location type
        // This could be based on policy type, field count, or other indicators
        int fieldCount = fieldSet.getFieldCount();
        if (fieldCount == LocationType.HOMEOWNER.getFieldCount()) {
            return LocationType.HOMEOWNER;
        }
        return LocationType.COMMERCIAL;
    }
