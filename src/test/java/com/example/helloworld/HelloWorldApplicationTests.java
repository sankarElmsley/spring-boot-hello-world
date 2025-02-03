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


public class PolicyLocationUpdateService {
    
    /**
     * Updates policy based on location information
     */
    public void updatePolicyWithLocationInfo(EdiPolicy policy, List<EdiLocation> locations, 
                                           List<EdiBusCode> busCodes, boolean isHomeowner, 
                                           boolean isCyberProd, boolean isHavingLocation) {
        
        if (isHomeowner) {
            updateHomeownerPolicy(policy, locations, busCodes);
        } else if (isCyberProd) {
            updateCyberPolicy(policy, locations, busCodes, isHavingLocation);
        } else {
            updateStandardPolicy(policy, locations, busCodes);
        }
    }
    
    /**
     * Updates homeowner policy with location information
     */
    private void updateHomeownerPolicy(EdiPolicy policy, List<EdiLocation> locations, List<EdiBusCode> busCodes) {
        // Find location with maximum value
        EdiLocation maxValueLocation = findLocationWithMaxValue(locations, busCodes, policy.getEdiCompNo());
        
        if (maxValueLocation != null) {
            policy.setEdiBusCode(maxValueLocation.getEdiLocBusCode());
            policy.setEdiBusSub(0);
            policy.setEdiBmType(maxValueLocation.getEdiLocCov());
        }
    }
    
    /**
     * Updates cyber product policy
     */
    private void updateCyberPolicy(EdiPolicy policy, List<EdiLocation> locations, 
                                 List<EdiBusCode> busCodes, boolean isHavingLocation) {
        if (!isHavingLocation) {
            // Create new location from insured info
            EdiLocation newLocation = createLocationFromInsured(policy);
            if (newLocation != null) {
                locations.add(newLocation);
            }
            
            // Update policy with default values
            policy.setEdiBusCode("1");
            policy.setEdiBusSub(0);
            
        } else {
            // Find and update with max value location
            EdiLocation maxValueLocation = findLocationWithMaxValue(locations, busCodes, policy.getEdiCompNo());
            if (maxValueLocation != null) {
                policy.setEdiBusCode(maxValueLocation.getEdiLocBusCode());
                policy.setEdiBusSub(0);
            }
        }
    }
    
    /**
     * Updates standard policy with location information
     */
    private void updateStandardPolicy(EdiPolicy policy, List<EdiLocation> locations, List<EdiBusCode> busCodes) {
        EdiLocation maxValueLocation = findLocationWithMaxValue(locations, busCodes, policy.getEdiCompNo());
        
        if (maxValueLocation != null) {
            policy.setEdiBusCode(maxValueLocation.getEdiLocBusCode());
            policy.setEdiBusSub(0);
        }
    }
    
    /**
     * Creates a new location from insured information
     */
    private EdiLocation createLocationFromInsured(EdiPolicy policy) {
        // Get insured information from policy
        EdiInsured insured = policy.getEdiInsured();
        if (insured == null) {
            return null;
        }
        
        EdiLocation location = new EdiLocation();
        location.setEdiRecNo(policy.getEdiRecNo());
        location.setEdiLocNo(insured.getEdiInsNo());
        location.setEdiLocName(insured.getEdiInsName());
        location.setEdiLocAdd(insured.getEdiInsAdd());
        location.setEdiLocCity(insured.getEdiInsCity());
        location.setEdiLocProv(insured.getEdiInsProv());
        location.setEdiLocPostal(insured.getEdiInsPostal());
        location.setEdiLocBusCode("1");
        location.setEdiLocBusSub(0);
        location.setEdiLocBmLoss("N");
        
        // Set creation/update info
        location.setEdiCDate(policy.getEdiCDate());
        location.setEdiUDate(policy.getEdiUDate());
        location.setEdiCUser(policy.getEdiCUser());
        location.setEdiUUser(policy.getEdiUUser());
        
        return location;
    }
    
    /**
     * Finds location with maximum value that matches business code criteria
     */
    private EdiLocation findLocationWithMaxValue(List<EdiLocation> locations, 
                                               List<EdiBusCode> busCodes, 
                                               String companyNo) {
        return locations.stream()
            .filter(loc -> isValidBusinessCode(loc, busCodes, companyNo))
            .max(Comparator.comparing(loc -> 
                loc.getEdiLocIlValue() != null ? 
                loc.getEdiLocIlValue() : 
                (loc.getEdiLocBValue() + loc.getEdiLocCValue())))
            .orElse(null);
    }
    
    /**
     * Validates business code for location
     */
    private boolean isValidBusinessCode(EdiLocation location, List<EdiBusCode> busCodes, String companyNo) {
        return busCodes.stream()
            .anyMatch(code -> 
                code.getEdiBusCode().equals(location.getEdiLocBusCode()) &&
                code.getEdiBusSub().equals(location.getEdiLocBusSub()) &&
                code.getEdiCompany().equals(companyNo));
    }
    
    /**
     * Example usage method showing how to use this service
     */
    public void exampleUsage() {
        PolicyLocationUpdateService service = new PolicyLocationUpdateService();
        
        // Example data setup
        EdiPolicy policy = new EdiPolicy();
        List<EdiLocation> locations = new ArrayList<>();
        List<EdiBusCode> busCodes = new ArrayList<>();
        
        // Update policy based on type
        service.updatePolicyWithLocationInfo(
            policy,
            locations,
            busCodes,
            false,  // isHomeowner
            true,   // isCyberProd
            false   // isHavingLocation
        );
    }
}
