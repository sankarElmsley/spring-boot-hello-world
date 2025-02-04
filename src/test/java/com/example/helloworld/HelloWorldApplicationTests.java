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



/**
 * Checks if HSP and SLC commissions are different for a policy and sets status to pending if they differ
 * @param recno The policy record number
 * @param edilocvals List of EDILOCVAL records for the policy
 * @return true if commissions are different, false otherwise
 */
private boolean checkHspSlcCommissions(String recno, List<EDILOCVAL> edilocvals) {
    // Group EDILOCVAL records by location property type (HSP vs SLC)
    Map<String, List<Double>> commissionsByType = edilocvals.stream()
        .filter(val -> val.getEDILOCPROPTYPE() != null && 
                      (val.getEDILOCPROPTYPE().equals(HSP) || 
                       val.getEDILOCPROPTYPE().equals(SLC)))
        .collect(Collectors.groupingBy(
            EDILOCVAL::getEDILOCPROPTYPE,
            Collectors.mapping(EDILOCVAL::getEDILOCCOMMISSION, Collectors.toList())
        ));
    
    // Get unique commission values for each type
    Map<String, Set<Double>> uniqueCommissionsByType = commissionsByType.entrySet()
        .stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            e -> e.getValue().stream().collect(Collectors.toSet())
        ));
    
    // Check if we have both HSP and SLC records
    if (uniqueCommissionsByType.containsKey(HSP) && 
        uniqueCommissionsByType.containsKey(SLC)) {
            
        // Get the commission values
        Set<Double> hspCommissions = uniqueCommissionsByType.get(HSP);
        Set<Double> slcCommissions = uniqueCommissionsByType.get(SLC);
        
        // Compare the commission values
        boolean commissionsAreDifferent = !hspCommissions.equals(slcCommissions);
        
        if (commissionsAreDifferent) {
            // Set policy status to Pending
            nokPolicy(recno, "P");
            
            // Log the difference
            String message = String.format(
                "Different commissions found - HSP: %s, SLC: %s", 
                hspCommissions.toString(), 
                slcCommissions.toString()
            );
            ediLogUpdate(recno, "I", message);
            
            return true;
        }
    }
    
    return false;
}
