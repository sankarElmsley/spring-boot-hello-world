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




@Service
@Slf4j
public class LocationValueGenerator {

    public List<EDILocationValue> generateLocationValues(Location location, String recNo) {
        List<EDILocationValue> values = new ArrayList<>();
        int locVal = 0;
        
        // For Homeowners
        if (LocationType.HOMEOWNER.equals(location.getLocationType())) {
            String bmCover = location.getLocBMCov();
            
            if (SGIConstants.HSP.equals(bmCover)) {
                values.add(createHSPSLCValue(location, recNo, ++locVal, bmCover, true));
            } else if (SGIConstants.SLC.equals(bmCover)) {
                values.add(createHSPSLCValue(location, recNo, ++locVal, bmCover, false));
            } else if (SGIConstants.HSPSLC.equals(bmCover)) {
                values.add(createHSPSLCValue(location, recNo, ++locVal, bmCover, true));  // HSP
                values.add(createHSPSLCValue(location, recNo, ++locVal, bmCover, false)); // SLC
            }
        }
        // For Commercial - Create value for each BI Form present
        else {
            if (hasValidBIForm(location.getLocBIForm1(), location.getLocBILimit1())) {
                values.add(createBIFormValue(location, recNo, ++locVal, 
                    location.getLocBIForm1(), location.getLocBILimit1(), location.getLocBuildingLimit()));
            }
            if (hasValidBIForm(location.getLocBIForm2(), location.getLocBILimit2())) {
                values.add(createBIFormValue(location, recNo, ++locVal,
                    location.getLocBIForm2(), location.getLocBILimit2(), location.getLocBuildingLimit()));
            }
            if (hasValidBIForm(location.getLocBIForm3(), location.getLocBILimit3())) {
                values.add(createBIFormValue(location, recNo, ++locVal,
                    location.getLocBIForm3(), location.getLocBILimit3(), location.getLocBuildingLimit()));
            }
            if (hasValidBIForm(location.getLocBIForm4(), location.getLocBILimit4())) {
                values.add(createBIFormValue(location, recNo, ++locVal,
                    location.getLocBIForm4(), location.getLocBILimit4(), location.getLocBuildingLimit()));
            }
            if (hasValidBIForm(location.getLocBIForm5(), location.getLocBILimit5())) {
                values.add(createBIFormValue(location, recNo, ++locVal,
                    location.getLocBIForm5(), location.getLocBILimit5(), location.getLocBuildingLimit()));
            }
            if (hasValidBIForm(location.getLocBIForm6(), location.getLocBILimit6())) {
                values.add(createBIFormValue(location, recNo, ++locVal,
                    location.getLocBIForm6(), location.getLocBILimit6(), location.getLocBuildingLimit()));
            }
        }
        
        return values;
    }

    private EDILocationValue createHSPSLCValue(Location location, String recNo, 
            int locVal, String bmCover, boolean isHSP) {
            
        EDILocationValue.EDILocationValueBuilder builder = EDILocationValue.builder()
            .ediRecNo(Integer.parseInt(recNo))
            .locNo(Integer.parseInt(location.getLineNumber()))
            .locVal(locVal)
            .locBMCov("0");

        if (isHSP) {
            builder
                .locAPrem(location.getLocHSPFTPrem())
                .locPremium(location.getLocHSPPremWrit())
                .locCommission(location.getLocHSPComm())
                .locDeduct(location.getLocHSPDeduct())
                .locILValue(location.getLocHSPLimit())
                .locPropType(SGIConstants.HSP);
        } else {
            builder
                .locAPrem(location.getLocSLCFTPrem())
                .locPremium(location.getLocSLCPremWrit())
                .locCommission(location.getLocSLCComm())
                .locDeduct(location.getLocSLCDeduct())
                .locILValue(location.getLocSLCLimit())
                .locPropType(SGIConstants.SLC);
        }

        return builder.build();
    }

    private EDILocationValue createBIFormValue(Location location, String recNo, 
            int locVal, String biForm, Double biLimit, Double buildingValue) {
            
        return EDILocationValue.builder()
            .ediRecNo(Integer.parseInt(recNo))
            .locNo(Integer.parseInt(location.getLineNumber()))
            .locVal(locVal)
            .locBIForm(lookupCoverageCodeDesc(biForm))
            .locBILimit(biLimit)
            .locBIVal(buildingValue)
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
