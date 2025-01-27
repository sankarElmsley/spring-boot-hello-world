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
    public List<EDILocationValue> processLocation(FieldSet fieldSet, String recNo) {
        try {
            // 1. Determine location type
            LocationType locationType = determineLocationType(fieldSet);
            
            // 2. Create location from fieldset
            Location location = locationService.createLocation(fieldSet, locationType);
            
            // 3. Generate location values
            return locationValueGenerator.generateLocationValues(location, recNo);
            
        } catch (Exception e) {
            log.error("Error processing location for recNo {}: {}", recNo, e.getMessage());
            throw new LocationProcessingException("Failed to process location", e);
        }
    }

    private LocationType determineLocationType(FieldSet fieldSet) {
        String packageType = fieldSet.readString("PolPackType");
        boolean isHomeowner = isHomeownerPackage(packageType);
        return isHomeowner ? LocationType.HOMEOWNER : LocationType.COMMERCIAL;
    }

    private boolean isHomeownerPackage(String packageType) {
        return SGIConstants.HOMEOWNER_PACKAGE_TYPES.contains(packageType);
    }
}

@Service
@Slf4j
@RequiredArgsConstructor
public class LocationValueGenerator {

    public List<EDILocationValue> generateLocationValues(Location location, String recNo) {
        List<EDILocationValue> values = new ArrayList<>();
        
        if (LocationType.HOMEOWNER.equals(location.getLocationType())) {
            values.addAll(generateHomeownerValues(location, recNo));
        } else {
            values.addAll(generateCommercialValues(location, recNo));
        }
        
        return values;
    }

    private List<EDILocationValue> generateHomeownerValues(Location location, String recNo) {
        List<EDILocationValue> values = new ArrayList<>();
        int locVal = 0;
        String bmCover = location.getLocBMCov();

        if (SGIConstants.HSP.equals(bmCover) || SGIConstants.HSPSLC.equals(bmCover)) {
            values.add(createHSPSLCValue(location, recNo, ++locVal, true));
        }
        
        if (SGIConstants.SLC.equals(bmCover) || SGIConstants.HSPSLC.equals(bmCover)) {
            values.add(createHSPSLCValue(location, recNo, ++locVal, false));
        }

        return values;
    }

    private List<EDILocationValue> generateCommercialValues(Location location, String recNo) {
        List<EDILocationValue> values = new ArrayList<>();
        int locVal = 0;

        // Process each BI Form if present
        if (hasValidBIForm(location.getLocBIForm1(), location.getLocBILimit1())) {
            values.add(createBIFormValue(location, recNo, ++locVal, 
                location.getLocBIForm1(), location.getLocBILimit1()));
        }
        
        if (hasValidBIForm(location.getLocBIForm2(), location.getLocBILimit2())) {
            values.add(createBIFormValue(location, recNo, ++locVal,
                location.getLocBIForm2(), location.getLocBILimit2()));
        }
        
        // Continue for BI Forms 3-6...
        if (hasValidBIForm(location.getLocBIForm3(), location.getLocBILimit3())) {
            values.add(createBIFormValue(location, recNo, ++locVal,
                location.getLocBIForm3(), location.getLocBILimit3()));
        }
        
        // ... add forms 4-6 similarly

        return values;
    }

    private EDILocationValue createHSPSLCValue(Location location, String recNo, 
            int locVal, boolean isHSP) {
            
        return EDILocationValue.builder()
            .ediRecNo(Integer.parseInt(recNo))
            .locNo(Integer.parseInt(location.getLineNumber()))
            .locVal(locVal)
            .locBMCov("0")
            .locAPrem(isHSP ? location.getLocHSPFTPrem() : location.getLocSLCFTPrem())
            .locPremium(isHSP ? location.getLocHSPPremWrit() : location.getLocSLCPremWrit())
            .locCommission(isHSP ? location.getLocHSPComm() : location.getLocSLCComm())
            .locDeduct(isHSP ? location.getLocHSPDeduct() : location.getLocSLCDeduct())
            .locILValue(isHSP ? location.getLocHSPLimit() : location.getLocSLCLimit())
            .locPropType(isHSP ? SGIConstants.HSP : SGIConstants.SLC)
            .build();
    }

    private EDILocationValue createBIFormValue(Location location, String recNo, 
            int locVal, String biForm, Double biLimit) {
            
        return EDILocationValue.builder()
            .ediRecNo(Integer.parseInt(recNo))
            .locNo(Integer.parseInt(location.getLineNumber()))
            .locVal(locVal)
            .locBIForm(lookupCoverageCodeDesc(biForm))
            .locBILimit(biLimit)
            .locBIVal(location.getLocBuildingLimit())
            .locPropType("BI")
            .build();
    }

    private boolean hasValidBIForm(String form, Double limit) {
        return StringUtils.isNotEmpty(form) && limit != null;
    }

    private String lookupCoverageCodeDesc(String code) {
        Map<String, String> coverageCodes = Map.of(
            "691", "Blanket Business Interruption Insurance (Gross Earnings Form)",
            "692", "Blanket Business Interruption Insurance (Profits Form)",
            "272", "Blanket Earnings Insurance (No Co-Insurance Form)",
            "313", "Blanket Extra Expense Insurance",
            "561", "Blanket Rent or Rental Value Insurance",
            "127", "Business Interruption (Gross Earnings Form)",
            "126", "Business Interruption (Gross Rentals Form)",
            "128", "Business Interruption (Profits Form)"
        );
        return coverageCodes.getOrDefault(code, code);
    }
}

@Data
@Builder
public class Location {
    // Line details
    private String lineName;
    private String lineNumber;
    private String lineNumberText;
    private String locLineNo;
    
    // Location identification
    private String locName;
    private String locAddressType;
    private String locParcel;
    private String locLot;
    private String locBlock;
    private String locPlan;
    
    // Geographic details
    private String locQuarter;
    private String locSection;
    private String locTownship;
    private String locRange;
    private String locMeridian;
    
    // Address details
    private String locCivSuiteNo;
    private String locCivStreetNo;
    private String locCivStreetName;
    private String locStreetCode;
    private String locStreetDirection;
    private String locLocationDesc;
    
    // City and postal details
    private String locCity;
    private String locProv;
    private String locPostCode;
    
    // Location indicators
    private String locNearInd;
    private String locNearLocName;
    private String locWithinLocName;
    
    // Business and coverage details
    private String locationBusCode;
    private String locBMCov;
    private String locBMChgCd;
    
    // Homeowner specific fields
    private Double locHSPFTPrem;
    private Double locHSPPremWrit;
    private Double locHSPComm;
    private Double locHSPDeduct;
    private Double locHSPLimit;
    private Double locSLCFTPrem;
    private Double locSLCPremWrit;
    private Double locSLCComm;
    private Double locSLCDeduct;
    private Double locSLCLimit;
    
    // Commercial specific fields
    private Double locBuildingLimit;
    private Double locDeduct;
    private Double locContentsLimit;
    private Double locContentsDeduct;
    private Double polCONLimit;
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
    
    // Type
    private LocationType locationType;
}

@Data
@Builder
public class EDILocationValue {
    private Integer ediRecNo;
    private Integer locNo;
    private Integer locVal;
    private Double locAPrem;
    private Double locPremium;
    private Double locCommission; 
    private Double locDeduct;
    private Double locILValue;
    private String locPropType;
    private String locBMCov;
    private String locBIForm;
    private Double locBILimit;
    private Double locBIVal;
}

public class SGIConstants {
    public static final String HSP = "HSP";
    public static final String SLC = "SLC";
    public static final String HSPSLC = "HSP/SLC";
    public static final String HOMEOWNERS = "HOMEOWNERS";
    public static final String NUMBER_ONE = "1";
    
    public static final Set<String> HOMEOWNER_PACKAGE_TYPES = Set.of(
        "HOA", "HOB", "HOC", "HOD", "HOE", "HOF"
    );
}

