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
                                           boolean isHomeowner, boolean isCyberProd, 
                                           boolean isHavingLocation) {
        
        if (isHomeowner) {
            updateHomeownerPolicy(policy, locations);
        } else if (isCyberProd) {
            updateCyberPolicy(policy, locations, isHavingLocation);
        } else {
            updateStandardPolicy(policy, locations);
        }
    }
    
    /**
     * Updates homeowner policy with location information
     */
    private void updateHomeownerPolicy(EdiPolicy policy, List<EdiLocation> locations) {
        // Find location with maximum value
        EdiLocation maxValueLocation = findLocationWithMaxValue(locations);
        
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
                                 boolean isHavingLocation) {
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
            EdiLocation maxValueLocation = findLocationWithMaxValue(locations);
            if (maxValueLocation != null) {
                policy.setEdiBusCode(maxValueLocation.getEdiLocBusCode());
                policy.setEdiBusSub(0);
            }
        }
    }
    
    /**
     * Updates standard policy with location information
     */
    private void updateStandardPolicy(EdiPolicy policy, List<EdiLocation> locations) {
        EdiLocation maxValueLocation = findLocationWithMaxValue(locations);
        
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
     * Finds location with maximum ilvalue, matching the SQL query:
     * SELECT l.edirecno, max(edilocilvalue) maxilvalue
     * FROM edilocation l
     */
    private EdiLocation findLocationWithMaxValue(List<EdiLocation> locations) {
        return locations.stream()
            .max(Comparator.comparing(EdiLocation::getEdiLocIlValue))
            .orElse(null);
    }
}
