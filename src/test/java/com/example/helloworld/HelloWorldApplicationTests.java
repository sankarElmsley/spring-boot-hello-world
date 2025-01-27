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


@Service
@Transactional
@Slf4j
public class LocationProcessor {
    
    private final ConnectionManager ejcm;
    
    public LocationProcessor(ConnectionManager ejcm) {
        this.ejcm = ejcm;
    }

    /**
     * Process a single location with all its associated location values
     */
    public void processLocationWithValues(EDILocationRecord locationRec, String recNo, boolean isHomeowner) {
        try {
            // 1. Create and save the parent location first
            EDILOCATION location = createLocation(locationRec, recNo);
            int locationNo = saveLocation(location);

            // 2. Create all associated location values referencing this location
            List<EDILOCVAL> locationValues = new ArrayList<>();
            
            if (isHomeowner) {
                // For homeowners, handle HSP/SLC values
                locationValues.addAll(createHomeownerLocationValues(locationRec, recNo, locationNo));
            } else {
                // For commercial, handle BI form values
                locationValues.addAll(createCommercialLocationValues(locationRec, recNo, locationNo));
            }

            // 3. Save all location values
            saveLocationValues(locationValues);
            
            log.info("Processed location {} with {} location values", locationNo, locationValues.size());

        } catch (SQLException e) {
            log.error("Error processing location for recNo {}: {}", recNo, e.getMessage());
            throw new LocationProcessingException("Failed to process location", e);
        }
    }

    private EDILOCATION createLocation(EDILocationRecord locationRec, String recNo) {
        EDILOCATION location = new EDILOCATION();
        
        // Set basic location info
        location.setEDIRECNO(Integer.parseInt(recNo));
        location.setEDILOCNO(locationRec.getLocationNo());
        location.setEDILOCADD(locationRec.getAddress());
        
        // Set location description if applicable
        if (StringUtils.isEmpty(location.getEDILOCADD()) && 
            StringUtils.isNotEmpty(locationRec.LocLocationDesc)) {
            String desc = locationRec.LocLocationDesc;
            if (desc.length() > 64) {
                location.setEDILOCADD(desc.substring(0, 62) + "**");
                location.setEDILOCLOCATIONDESC(desc);
            } else {
                location.setEDILOCADD(desc);
            }
        }

        // Set business fields
        location.setEDILOCNAME("Building #" + locationRec.LocName);
        location.setEDILOCBUSCODE(locationRec.LocBusCode);
        location.setEDILOCCOV(locationRec.LocBMCov);
        location.setEDILOCBIFORM(locationRec.LocBIForm1); // Primary BI form

        // Set limits and values
        if (!"NULL".equals(locationRec.LocDeduct)) {
            location.setEDILOCDEDUCT(Double.parseDouble(locationRec.LocDeduct));
        }
        if (!"NULL".equals(locationRec.LocBuildingLimit)) {
            location.setEDILOCBVALUE(Double.parseDouble(locationRec.LocBuildingLimit));
        }
        if (!"NULL".equals(locationRec.LocContentsLimit)) {
            location.setEDILOCCVALUE(Double.parseDouble(locationRec.LocContentsLimit));
        }

        // Set audit fields
        location.setEDICDATE(getCurrentTimestamp());
        location.setEDIUDATE(getCurrentTimestamp());
        location.setEDICUSER(getCurrentUser());
        location.setEDIUUSER(getCurrentUser());
        
        return location;
    }

    private List<EDILOCVAL> createHomeownerLocationValues(EDILocationRecord locationRec, 
            String recNo, int locationNo) {
        List<EDILOCVAL> values = new ArrayList<>();
        int locVal = 0;
        String bmCover = locationRec.LocBMCov;

        // Create HSP value if applicable
        if ("HSP".equals(bmCover) || "HSP/SLC".equals(bmCover)) {
            values.add(createHSPSLCValue(locationRec, recNo, locationNo, ++locVal, true));
        }

        // Create SLC value if applicable
        if ("SLC".equals(bmCover) || "HSP/SLC".equals(bmCover)) {
            values.add(createHSPSLCValue(locationRec, recNo, locationNo, ++locVal, false));
        }

        return values;
    }

    private List<EDILOCVAL> createCommercialLocationValues(EDILocationRecord locationRec, 
            String recNo, int locationNo) {
        List<EDILOCVAL> values = new ArrayList<>();
        int locVal = 0;

        // Check each BI form and create corresponding location value
        if (isValidBIForm(locationRec.LocBIForm1, locationRec.LocBILimit1)) {
            values.add(createBIFormValue(locationRec, recNo, locationNo, ++locVal,
                locationRec.LocBIForm1, locationRec.LocBILimit1));
        }
        if (isValidBIForm(locationRec.LocBIForm2, locationRec.LocBILimit2)) {
            values.add(createBIFormValue(locationRec, recNo, locationNo, ++locVal,
                locationRec.LocBIForm2, locationRec.LocBILimit2));
        }
        if (isValidBIForm(locationRec.LocBIForm3, locationRec.LocBILimit3)) {
            values.add(createBIFormValue(locationRec, recNo, locationNo, ++locVal,
                locationRec.LocBIForm3, locationRec.LocBILimit3));
        }
        // ... handle forms 4-6 similarly

        return values;
    }

    private EDILOCVAL createHSPSLCValue(EDILocationRecord locationRec, String recNo, 
            int locationNo, int locVal, boolean isHSP) {
        EDILOCVAL value = new EDILOCVAL();
        
        // Set parent location reference
        value.setEDIRECNO(Integer.parseInt(recNo));
        value.setEDILOCNO(locationNo);
        value.setEDILOCVAL(locVal);
        value.setEDILOCBMCOV("0");

        if (isHSP) {
            value.setEDILOCAPREM(parseDouble(locationRec.LocHSPFTPrem));
            value.setEDILOCPREMIUM(parseDouble(locationRec.LocHSPPremWrit));
            value.setEDILOCCOMMISSION(parseDouble(locationRec.LocHSPComm));
            value.setEDILOCDEDUCT(parseDouble(locationRec.LocHSPDeduct));
            value.setEDILOCILVALUE(parseDouble(locationRec.LocHSPLimit));
            value.setEDILOCPROPTYPE("HSP");
        } else {
            value.setEDILOCAPREM(parseDouble(locationRec.LocSLCFTPrem));
            value.setEDILOCPREMIUM(parseDouble(locationRec.LocSLCPremWrit));
            value.setEDILOCCOMMISSION(parseDouble(locationRec.LocSLCComm));
            value.setEDILOCDEDUCT(parseDouble(locationRec.LocSLCDeduct));
            value.setEDILOCILVALUE(parseDouble(locationRec.LocSLCLimit));
            value.setEDILOCPROPTYPE("SLC");
        }

        // Set audit fields
        value.setEDICDATE(getCurrentTimestamp());
        value.setEDIUDATE(getCurrentTimestamp());
        value.setEDICUSER(getCurrentUser());
        value.setEDIUUSER(getCurrentUser());

        return value;
    }

    private EDILOCVAL createBIFormValue(EDILocationRecord locationRec, String recNo, 
            int locationNo, int locVal, String biForm, String biLimit) {
        EDILOCVAL value = new EDILOCVAL();
        
        // Set parent location reference
        value.setEDIRECNO(Integer.parseInt(recNo));
        value.setEDILOCNO(locationNo);
        value.setEDILOCVAL(locVal);
        
        // Set BI form specific values
        value.setEDILOCBIFORM(lookupCoverageCodeDesc(biForm));
        value.setEDILOCBILIMIT(Double.parseDouble(biLimit));
        value.setEDILOCBIVAL(parseDouble(locationRec.LocBuildingLimit));
        value.setEDILOCPROPTYPE("BI");

        // Set audit fields
        value.setEDICDATE(getCurrentTimestamp());
        value.setEDIUDATE(getCurrentTimestamp());
        value.setEDICUSER(getCurrentUser());
        value.setEDIUUSER(getCurrentUser());

        return value;
    }

    private int saveLocation(EDILOCATION location) throws SQLException {
        ejcm.writeRow(location);
        log.info("Saved location - RecNo: {}, LocNo: {}", 
            location.getEDIRECNO(), location.getEDILOCNO());
        return location.getEDILOCNO();
    }

    private void saveLocationValues(List<EDILOCVAL> values) throws SQLException {
        for (EDILOCVAL value : values) {
            ejcm.writeRow(value);
            log.info("Saved location value - RecNo: {}, LocNo: {}, LocVal: {}", 
                value.getEDIRECNO(), value.getEDILOCNO(), value.getEDILOCVAL());
        }
    }

    private boolean isValidBIForm(String form, String limit) {
        return !"NULL".equals(form) && !"NULL".equals(limit);
    }

    private Double parseDouble(String value) {
        return "NULL".equals(value) ? 0.0 : Double.parseDouble(value);
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
}
