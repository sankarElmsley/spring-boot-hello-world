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



public class PolicyUpdateService {
    private static final String HSP = "HSP";
    private static final String SLC = "SLC";
    private static final String HSPSLC = "HSP/SLC";
    private static final String HOMEOWNERS = "HOMEOWNERS";

    public void updatePolicyDeductibleAndType(Policy policy, List<Location> locations, 
                                            List<Policy> allPolicies) {
        // Update deductible if it's zero or null
        if (policy.getDeductible() == null || policy.getDeductible() == 0) {
            locations.stream()
                .map(Location::getDeductible)
                .filter(Objects::nonNull)
                .max(Double::compare)
                .ifPresent(policy::setDeductible);
        }

        // Handle BMType updates for homeowner policies
        if (isHomeownerPolicyWithRevisionOrCancellation(policy)) {
            handleHomeownersPolicy(policy, allPolicies);
        }
    }

    private boolean isHomeownerPolicyWithRevisionOrCancellation(Policy policy) {
        return HOMEOWNERS.equalsIgnoreCase(policy.getBmtype()) && 
               Arrays.asList("2", "4").contains(policy.getTrnCode());
    }

    private void handleHomeownersPolicy(Policy currentPolicy, List<Policy> allPolicies) {
        allPolicies.stream()
            .filter(p -> p.getEdipolno().equals(currentPolicy.getEdipolno()))
            .filter(p -> Integer.parseInt(p.getRecNo()) < Integer.parseInt(currentPolicy.getRecNo()))
            .filter(p -> !"X".equals(p.getStatus()))
            .max(Comparator.comparing(p -> Integer.parseInt(p.getRecNo())))
            .ifPresent(previousPolicy -> updateBasedOnPreviousPolicy(currentPolicy, previousPolicy));
    }

    private void updateBasedOnPreviousPolicy(Policy currentPolicy, Policy previousPolicy) {
        String previousBmtype = previousPolicy.getBmtype();
        
        if (StringUtils.isBlank(previousBmtype)) {
            currentPolicy.setStatus("N");
            logError(currentPolicy.getRecNo(), "The bmtype for the previous edirecno was empty!");
            return;
        }

        previousBmtype = previousBmtype.trim();
        if (HOMEOWNERS.equalsIgnoreCase(previousBmtype)) {
            currentPolicy.setStatus("N");
            logError(currentPolicy.getRecNo(), 
                "The bmtype for the previous edirecno was not properly set!");
        } else if (isValidBmType(previousBmtype)) {
            currentPolicy.setBmtype(previousBmtype);
            logInfo(currentPolicy.getRecNo(), "BMType updated successfully");
        } else {
            currentPolicy.setStatus("N");
            logError(currentPolicy.getRecNo(), 
                "The bmtype for the previous edirecno was wrong!");
        }
    }

    private boolean isValidBmType(String bmtype) {
        return HSP.equals(bmtype) || SLC.equals(bmtype) || HSPSLC.equals(bmtype);
    }

    private void logError(String recNo, String message) {
        // Implementation of error logging
    }

    private void logInfo(String recNo, String message) {
        // Implementation of info logging
    }
}
