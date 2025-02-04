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


public class PolicyDeductibleUpdateService {
    
    private static final String HOMEOWNERS = "HOMEOWNERS";
    private static final String HSP = "HSP";
    private static final String SLC = "SLC";
    private static final String HSPSLC = "HSPSLC";
    
    /**
     * Updates policy deductible and bmtype based on locations and previous policies
     */
    public void updatePolicyDeductibleAndType(EdiPolicy currentPolicy, 
                                            List<EdiLocation> locations,
                                            List<EdiPolicy> previousPolicies) {
        
        // Update policy deductible if it's 0 or null
        updatePolicyDeductible(currentPolicy, locations);
        
        // Update bmtype for homeowners policies
        if (isHomeownersRevisionOrCancellation(currentPolicy)) {
            updateHomeownersBmType(currentPolicy, previousPolicies);
        }
    }
    
    /**
     * Updates policy deductible with highest location deductible if current is 0 or null
     */
    private void updatePolicyDeductible(EdiPolicy policy, List<EdiLocation> locations) {
        if (policy.getEdiBmDeduct() == null || policy.getEdiBmDeduct() == 0) {
            locations.stream()
                .filter(loc -> loc.getEdiRecNo().equals(policy.getEdiRecNo()))
                .mapToDouble(EdiLocation::getEdiLocDeduct)
                .max()
                .ifPresent(policy::setEdiBmDeduct);
        }
    }
    
    /**
     * Checks if policy is a homeowners revision or cancellation
     */
    private boolean isHomeownersRevisionOrCancellation(EdiPolicy policy) {
        return HOMEOWNERS.equalsIgnoreCase(policy.getEdiBmType()) &&
               Arrays.asList("2", "4").contains(policy.getEdiTrnCode());
    }
    
    /**
     * Updates homeowners policy bmtype based on previous policies
     */
    private void updateHomeownersBmType(EdiPolicy currentPolicy, List<EdiPolicy> previousPolicies) {
        previousPolicies.stream()
            .filter(prev -> isPreviousValidPolicy(prev, currentPolicy))
            .findFirst()
            .ifPresent(prevPolicy -> handlePreviousPolicy(currentPolicy, prevPolicy));
    }
    
    /**
     * Checks if a policy is a valid previous policy
     */
    private boolean isPreviousValidPolicy(EdiPolicy prev, EdiPolicy current) {
        return prev.getEdiRecNo() < current.getEdiRecNo() &&
               !"X".equals(prev.getEdiPolOk()) &&
               prev.getEdiPolNo().equals(current.getEdiPolNo());
    }
    
    /**
     * Handles the previous policy processing logic
     */
    private void handlePreviousPolicy(EdiPolicy currentPolicy, EdiPolicy prevPolicy) {
        String prevBmType = prevPolicy.getEdiBmType();
        
        if (prevBmType == null || prevBmType.trim().isEmpty()) {
            handleEmptyBmType(currentPolicy, prevPolicy.getEdiRecNo());
        } else {
            prevBmType = prevBmType.trim();
            if (HOMEOWNERS.equalsIgnoreCase(prevBmType)) {
                handleInvalidHomeownersBmType(currentPolicy, prevPolicy.getEdiRecNo());
            } else if (isValidSpecialBmType(prevBmType)) {
                updatePolicyBmType(currentPolicy, prevBmType);
            } else {
                handleInvalidBmType(currentPolicy, prevPolicy.getEdiRecNo());
            }
        }
    }
    
    /**
     * Checks if bmtype is a valid special type (HSP, SLC, HSPSLC)
     */
    private boolean isValidSpecialBmType(String bmType) {
        return HSP.equals(bmType) || SLC.equals(bmType) || HSPSLC.equals(bmType);
    }
    
    /**
     * Updates the policy bmtype and logs success
     */
    private void updatePolicyBmType(EdiPolicy policy, String newBmType) {
        policy.setEdiBmType(newBmType);
        logSuccess(policy.getEdiRecNo(), "The bmtype has been updated successfully!");
    }
    
    /**
     * Handles case where previous policy has empty bmtype
     */
    private void handleEmptyBmType(EdiPolicy policy, int prevRecNo) {
        markPolicyNotOk(policy);
        logError(policy.getEdiRecNo(), 
                "The bmtype for the previous edirecno " + prevRecNo + " was empty!");
    }
    
    /**
     * Handles case where previous policy has invalid homeowners bmtype
     */
    private void handleInvalidHomeownersBmType(EdiPolicy policy, int prevRecNo) {
        markPolicyNotOk(policy);
        logError(policy.getEdiRecNo(), 
                "The bmtype for the previous edirecno " + prevRecNo + " was not properly set!");
    }
    
    /**
     * Handles case where previous policy has invalid bmtype
     */
    private void handleInvalidBmType(EdiPolicy policy, int prevRecNo) {
        markPolicyNotOk(policy);
        logError(policy.getEdiRecNo(), 
                "The bmtype for the previous edirecno " + prevRecNo + " was wrong!");
    }
    
    /**
     * Marks policy as not ok
     */
    private void markPolicyNotOk(EdiPolicy policy) {
        policy.setEdiPolOk("N");
    }
    
    /**
     * Logs error message
     */
    private void logError(int recNo, String message) {
        // Replace with your actual logging mechanism
        System.err.println("Error for recNo " + recNo + ": " + message);
    }
    
    /**
     * Logs success message
     */
    private void logSuccess(int recNo, String message) {
        // Replace with your actual logging mechanism
        System.out.println("Success for recNo " + recNo + ": " + message);
    }
    
    /**
     * Example usage method
     */
    public void exampleUsage() {
        PolicyDeductibleUpdateService service = new PolicyDeductibleUpdateService();
        
        // Example data
        EdiPolicy currentPolicy = new EdiPolicy();
        List<EdiLocation> locations = new ArrayList<>();
        List<EdiPolicy> previousPolicies = new ArrayList<>();
        
        // Update policy
        service.updatePolicyDeductibleAndType(currentPolicy, locations, previousPolicies);
    }
}




public class PolicyService {
    
    private final Connection connection;
    
    public PolicyService(Connection connection) {
        this.connection = connection;
    }
    
    /**
     * Gets previous policies from database for a given policy
     * Returns empty list if no previous policies found
     */
    public List<EdiPolicy> getPreviousPolicies(String recNo, String ediPolNo) {
        List<EdiPolicy> previousPolicies = new ArrayList<>();
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            String query = "SELECT edibmtype, edirecno, edipolno, edipolok " +
                          "FROM dbo.edipolicy " +
                          "WHERE edirecno < ? " +
                          "AND edipolok != 'X' " +
                          "AND edipolno = ? " +
                          "ORDER BY edirecno DESC";
                          
            ps = connection.prepareStatement(query);
            ps.setString(1, recNo);
            ps.setString(2, ediPolNo);
            rs = ps.executeQuery();
            
            while (rs.next()) {
                EdiPolicy policy = mapResultSetToPolicy(rs);
                previousPolicies.add(policy);
            }
            
        } catch (SQLException e) {
            handleDatabaseError("Error fetching previous policies", e);
        } finally {
            closeResources(ps, rs);
        }
        
        return previousPolicies;
    }
    
    /**
     * Maps ResultSet to EdiPolicy object
     */
    private EdiPolicy mapResultSetToPolicy(ResultSet rs) throws SQLException {
        EdiPolicy policy = new EdiPolicy();
        policy.setEdiRecNo(rs.getInt("edirecno"));
        policy.setEdiPolNo(rs.getString("edipolno"));
        policy.setEdiBmType(rs.getString("edibmtype"));
        policy.setEdiPolOk(rs.getString("edipolok"));
        return policy;
    }
    
    /**
     * Handles database errors with proper logging
     */
    private void handleDatabaseError(String message, SQLException e) {
        String errorMessage = String.format("Database error: %s - %s", message, e.getMessage());
        // Replace with your actual logging mechanism
        System.err.println(errorMessage);
        e.printStackTrace();
    }
    
    /**
     * Closes database resources safely
     */
    private void closeResources(PreparedStatement ps, ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
            if (ps != null) {
                ps.close();
            }
        } catch (SQLException e) {
            handleDatabaseError("Error closing resources", e);
        }
    }
}

/**
 * Updated PolicyDeductibleUpdateService to work with database access
 */
public class PolicyDeductibleUpdateService {
    private final PolicyService policyService;
    private static final String HOMEOWNERS = "HOMEOWNERS";
    private static final String HSP = "HSP";
    private static final String SLC = "SLC";
    private static final String HSPSLC = "HSPSLC";
    
    public PolicyDeductibleUpdateService(Connection connection) {
        this.policyService = new PolicyService(connection);
    }
    
    /**
     * Main method to update policy deductible and bmtype
     */
    public void updatePolicyDeductibleAndType(EdiPolicy currentPolicy, List<EdiLocation> locations) {
        // Update policy deductible if it's 0 or null
        updatePolicyDeductible(currentPolicy, locations);
        
        // Update bmtype for homeowners policies
        if (isHomeownersRevisionOrCancellation(currentPolicy)) {
            // Fetch previous policies from database
            List<EdiPolicy> previousPolicies = policyService.getPreviousPolicies(
                String.valueOf(currentPolicy.getEdiRecNo()), 
                currentPolicy.getEdiPolNo()
            );
            
            updateHomeownersBmType(currentPolicy, previousPolicies);
        }
    }
    
    /**
     * Updates policy deductible with highest location deductible if current is 0 or null
     */
    private void updatePolicyDeductible(EdiPolicy policy, List<EdiLocation> locations) {
        if (policy.getEdiBmDeduct() == null || policy.getEdiBmDeduct() == 0) {
            locations.stream()
                .filter(loc -> loc.getEdiRecNo().equals(policy.getEdiRecNo()))
                .mapToDouble(EdiLocation::getEdiLocDeduct)
                .max()
                .ifPresent(policy::setEdiBmDeduct);
        }
    }
    
    /**
     * Checks if policy is a homeowners revision or cancellation
     */
    private boolean isHomeownersRevisionOrCancellation(EdiPolicy policy) {
        return HOMEOWNERS.equalsIgnoreCase(policy.getEdiBmType()) &&
               Arrays.asList("2", "4").contains(policy.getEdiTrnCode());
    }
    
    /**
     * Updates homeowners policy bmtype based on previous policies
     */
    private void updateHomeownersBmType(EdiPolicy currentPolicy, List<EdiPolicy> previousPolicies) {
        previousPolicies.stream()
            .findFirst()
            .ifPresent(prevPolicy -> handlePreviousPolicy(currentPolicy, prevPolicy));
    }
    
    // ... rest of the methods remain the same as in previous implementation ...
}

/**
 * Example usage
 */
public class Example {
    public void demonstrateUsage() {
        try (Connection connection = getConnection()) {  // Your connection method
            PolicyDeductibleUpdateService service = new PolicyDeductibleUpdateService(connection);
            
            // Your current policy and locations
            EdiPolicy currentPolicy = new EdiPolicy();
            List<EdiLocation> locations = new ArrayList<>();
            
            // Update policy
            service.updatePolicyDeductibleAndType(currentPolicy, locations);
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
