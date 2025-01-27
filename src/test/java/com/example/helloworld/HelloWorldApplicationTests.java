package com.example.helloworld;

import com.example.helloworld.controller.HelloWorldController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;


@SpringBootTest
class HelloWorldApplicationTests {

    @Autowired
    private HelloWorldController helloWorldController;

    @Test
    void contextLoads() {
        // to ensure that controller is getting created inside the application context
        assertNotNull(helloWorldController);
    }
}


@Slf4j
@Service
@RequiredArgsConstructor
public class SGILocationProcessor {
    
    private final SGILocationService locationService;
    private final LocationValueGenerator locationValueGenerator;

    /**
     * Main processing method that handles the complete location processing flow
     */
    public Map<Location, List<EDILocationValue>> processLocation(FieldSet fieldSet, String recNo) {
        try {
            // 1. Determine location type
            LocationType locationType = determineLocationType(fieldSet);
            
            // 2. Create multiple locations based on coverage types
            List<Location> locations = createLocations(fieldSet, locationType);
            
            // 3. Generate location values for each location
            Map<Location, List<EDILocationValue>> locationValuesMap = new HashMap<>();
            
            for (Location location : locations) {
                List<EDILocationValue> values = locationValueGenerator.generateLocationValues(location, recNo);
                locationValuesMap.put(location, values);
            }
            
            return locationValuesMap;
            
        } catch (Exception e) {
            log.error("Error processing location for recNo {}: {}", recNo, e.getMessage());
            throw new LocationProcessingException("Failed to process location", e);
        }
    }

    /**
     * Creates multiple locations based on coverage types
     */
    private List<Location> createLocations(FieldSet fieldSet, LocationType locationType) {
        List<Location> locations = new ArrayList<>();
        
        if (locationType == LocationType.HOMEOWNER) {
            locations.addAll(createHomeownerLocations(fieldSet));
        } else {
            locations.addAll(createCommercialLocations(fieldSet));
        }
        
        return locations;
    }

    /**
     * Creates Homeowner locations based on coverage types (HSP, SLC, or both)
     */
    private List<Location> createHomeownerLocations(FieldSet fieldSet) {
        List<Location> locations = new ArrayList<>();
        
        // Create base location from fieldset
        Location baseLocation = locationService.createLocation(fieldSet, LocationType.HOMEOWNER);
        
        // Check HSP values
        boolean hasHSP = hasValidHSPValues(baseLocation);
        // Check SLC values
        boolean hasSLC = hasValidSLCValues(baseLocation);
        
        if (hasHSP && hasSLC) {
            // Create separate locations for HSP and SLC
            locations.add(createHSPLocation(baseLocation));
            locations.add(createSLCLocation(baseLocation));
        } else if (hasHSP) {
            locations.add(createHSPLocation(baseLocation));
        } else if (hasSLC) {
            locations.add(createSLCLocation(baseLocation));
        }
        
        return locations;
    }

    /**
     * Creates Commercial locations based on BI Forms
     */
    private List<Location> createCommercialLocations(FieldSet fieldSet) {
        List<Location> locations = new ArrayList<>();
        
        // Create base location from fieldset
        Location baseLocation = locationService.createLocation(fieldSet, LocationType.COMMERCIAL);
        
        // Check each BI Form and create separate location if valid
        if (hasValidBIForm(baseLocation.getLocBIForm1(), baseLocation.getLocBILimit1())) {
            locations.add(createBIFormLocation(baseLocation, 1));
        }
        
        if (hasValidBIForm(baseLocation.getLocBIForm2(), baseLocation.getLocBILimit2())) {
            locations.add(createBIFormLocation(baseLocation, 2));
        }
        
        if (hasValidBIForm(baseLocation.getLocBIForm3(), baseLocation.getLocBILimit3())) {
            locations.add(createBIFormLocation(baseLocation, 3));
        }
        
        // Continue for BI Forms 4-6
        if (hasValidBIForm(baseLocation.getLocBIForm4(), baseLocation.getLocBILimit4())) {
            locations.add(createBIFormLocation(baseLocation, 4));
        }
        
        if (hasValidBIForm(baseLocation.getLocBIForm5(), baseLocation.getLocBILimit5())) {
            locations.add(createBIFormLocation(baseLocation, 5));
        }
        
        if (hasValidBIForm(baseLocation.getLocBIForm6(), baseLocation.getLocBILimit6())) {
            locations.add(createBIFormLocation(baseLocation, 6));
        }
        
        return locations;
    }

    private Location createHSPLocation(Location baseLocation) {
        return Location.builder()
            .lineName(baseLocation.getLineName())
            .lineNumber(baseLocation.getLineNumber())
            // Copy all base location fields
            // ...
            .locBMCov(SGIConstants.HSP)
            .locHSPFTPrem(baseLocation.getLocHSPFTPrem())
            .locHSPPremWrit(baseLocation.getLocHSPPremWrit())
            .locHSPComm(baseLocation.getLocHSPComm())
            .locHSPDeduct(baseLocation.getLocHSPDeduct())
            .locHSPLimit(baseLocation.getLocHSPLimit())
            .locationType(LocationType.HOMEOWNER)
            .build();
    }

    private Location createSLCLocation(Location baseLocation) {
        return Location.builder()
            .lineName(baseLocation.getLineName())
            .lineNumber(baseLocation.getLineNumber())
            // Copy all base location fields
            // ...
            .locBMCov(SGIConstants.SLC)
            .locSLCFTPrem(baseLocation.getLocSLCFTPrem())
            .locSLCPremWrit(baseLocation.getLocSLCPremWrit())
            .locSLCComm(baseLocation.getLocSLCComm())
            .locSLCDeduct(baseLocation.getLocSLCDeduct())
            .locSLCLimit(baseLocation.getLocSLCLimit())
            .locationType(LocationType.HOMEOWNER)
            .build();
    }

    private Location createBIFormLocation(Location baseLocation, int formNumber) {
        Location.LocationBuilder builder = Location.builder()
            .lineName(baseLocation.getLineName())
            .lineNumber(baseLocation.getLineNumber())
            // Copy all base location fields
            // ...
            .locationType(LocationType.COMMERCIAL);

        // Set specific BI form details
        switch (formNumber) {
            case 1:
                builder.locBIForm1(baseLocation.getLocBIForm1())
                      .locBILimit1(baseLocation.getLocBILimit1());
                break;
            case 2:
                builder.locBIForm2(baseLocation.getLocBIForm2())
                      .locBILimit2(baseLocation.getLocBILimit2());
                break;
            // Continue for forms 3-6
        }

        return builder.build();
    }

    private boolean hasValidHSPValues(Location location) {
        return location.getLocHSPFTPrem() != null && 
               location.getLocHSPPremWrit() != null;
    }

    private boolean hasValidSLCValues(Location location) {
        return location.getLocSLCFTPrem() != null && 
               location.getLocSLCPremWrit() != null;
    }

    private boolean hasValidBIForm(String form, Double limit) {
        return StringUtils.isNotEmpty(form) && limit != null;
    }

    private LocationType determineLocationType(FieldSet fieldSet) {
        String packageType = fieldSet.readString("PolPackType");
        boolean isHomeowner = SGIConstants.HOMEOWNER_PACKAGE_TYPES.contains(packageType);
        return isHomeowner ? LocationType.HOMEOWNER : LocationType.COMMERCIAL;
    }
}



