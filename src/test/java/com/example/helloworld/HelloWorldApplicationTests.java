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


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProductTypeDeterminer {
    
    private final Connection connection;
    private final Map<String, String> productMappings;
    private final HashSet<String> hs_SgiSip;
    
    // Constants
    private static final String STANDARD_PRODUCT_R1400 = "R1400";
    private static final String STANDARD_PRODUCT_R1300 = "R1300";
    private static final String CYBER_PRODUCT_RTF321 = "RTF321";
    private static final String CYBER_PRODUCT_RTF322 = "RTF322";
    private static final String CYBER_PRODUCT_RTF323 = "RTF323";
    private static final String DEFAULT_PRODUCT = "RTF18";
    
    // Transaction codes
    private static final String NEW_BUSINESS = "1";
    private static final String REVISION = "2";
    private static final String RENEWAL = "3";
    private static final String CANCELLATION = "4";
    private static final String LAPSE = "5";
    private static final String REINSTATEMENT = "7";
    
    @Data
    @Builder
    public static class Policy {
        private String policyNumber;
        private String bmType;
        private String bmTypeCov;
        private String packType;
        private int recno;
        private String transactionCode;
        private Double bmTermPremium;
        private Double bmAnnualPremium;
        private String polComm;
        private String previousPolicyNumber;
        private boolean isHomeowner;
    }
    
    public ProductTypeDeterminer(Connection connection, Map<String, String> productMappings, HashSet<String> hs_SgiSip) {
        this.connection = connection;
        this.productMappings = productMappings;
        this.hs_SgiSip = hs_SgiSip;
    }
    
    public String determineProductType(Policy policy) {
        String productId = "";
        boolean statusUpdated = false;
        
        try {
            // Determine product type
            if (isStandardProduct(policy)) {
                productId = handleStandardProduct(policy);
                statusUpdated = true;
            } else if (isCyberProduct(policy)) {
                productId = handleCyberProduct(policy);
            } else if (isHomeownerProduct(policy)) {
                productId = handleHomeownerProduct(policy);
            } else {
                productId = handleRegularProduct(policy);
            }
            
            // Handle status updates if not already done
            if (!statusUpdated) {
                handlePolicyStatus(policy);
            }
            
            // Handle SIP policies
            if (isSIPPolicy(policy)) {
                handleSIPPolicy(policy);
            }
            
            log.info("Determined product type {} for policy {}", productId, policy.getPolicyNumber());
            
        } catch (Exception e) {
            log.error("Failed to determine product type for policy: {}", policy.getPolicyNumber(), e);
            throw new ProductDeterminationException("Error determining product type", e);
        }
        
        return productId;
    }
    
    private boolean isStandardProduct(Policy policy) {
        String bmType = policy.getBmType();
        return (bmType != null && (bmType.equals("B") || bmType.equals("U")));
    }
    
    private String handleStandardProduct(Policy policy) throws SQLException {
        // Standard policy business code update
        if (NEW_BUSINESS.equals(policy.getTransactionCode()) || 
            RENEWAL.equals(policy.getTransactionCode())) {
            updateStandardPolicyBusinessCodes(policy);
        }
        
        // Handle policy status
        if (shouldUpdateStandardPolicyStatus(policy)) {
            if (hasInvalidBusinessCode(policy) || hasNoInsured(policy) || hasMissingPostalCode(policy)) {
                updatePolicyStatus(policy.getRecno(), "N");
            }
        }
        
        return STANDARD_PRODUCT_R1400;
    }
    
    private void updateStandardPolicyBusinessCodes(Policy policy) throws SQLException {
        String maxLimitLocationSql = 
            "SELECT edilocbuscode, edilocno FROM edilocation WHERE edirecno = ? " +
            "AND edilocilvalue = (SELECT MAX(edilocilvalue) FROM edilocation WHERE edirecno = ?)";
            
        try (PreparedStatement ps = connection.prepareStatement(maxLimitLocationSql)) {
            ps.setInt(1, policy.getRecno());
            ps.setInt(2, policy.getRecno());
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String businessCode = rs.getString("edilocbuscode");
                    updateAllLocationsBusinessCode(policy.getRecno(), businessCode);
                    updatePolicyBusinessCode(policy.getRecno(), businessCode);
                }
            }
        }
    }
    
    private void updateAllLocationsBusinessCode(int recno, String businessCode) throws SQLException {
        String sql = "UPDATE edilocation SET " +
                    "edilocbuscode = ?, edilocbussub = 0 " +
                    "WHERE edirecno = ? AND (edilocbuscode IS NULL OR NOT EXISTS " +
                    "(SELECT 1 FROM edibuscod WHERE edibuscode = edilocbuscode))";
                    
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, businessCode);
            ps.setInt(2, recno);
            ps.executeUpdate();
        }
    }
    
    private boolean isCyberProduct(Policy policy) {
        String bmType = policy.getBmType();
        if (bmType == null) return false;
        
        return bmType.contains("DC1") || bmType.contains("DC3") || 
               bmType.contains("C1") || bmType.contains("C3");
    }
    
    private String handleCyberProduct(Policy policy) {
        String bmType = policy.getBmType();
        
        // Combined cyber products
        if (bmType.contains("DC1") && bmType.contains("DC3")) {
            return CYBER_PRODUCT_RTF321;
        }
        if (bmType.contains("C1") && bmType.contains("C3")) {
            return CYBER_PRODUCT_RTF322;
        }
        
        // Single cyber components
        if (bmType.contains("DC1") || bmType.contains("DC3")) {
            return CYBER_PRODUCT_RTF323;
        }
        
        // Special case for revision with no premium
        if (REVISION.equals(policy.getTransactionCode()) && 
            policy.getBmTermPremium() != null && 
            policy.getBmTermPremium() == 0) {
            return "10"; // Non-premium endorsement
        }
        
        return DEFAULT_PRODUCT;
    }
    
    private boolean isHomeownerProduct(Policy policy) {
        return policy.isHomeowner();
    }
    
    private String handleHomeownerProduct(Policy policy) throws SQLException {
        // Special handling for homeowner products
        if (policy.getPackType() != null) {
            String homeownerType = determineHomeownerType(policy);
            updateHomeownerStatus(policy, homeownerType);
            return homeownerType;
        }
        return DEFAULT_PRODUCT;
    }
    
    private void handlePolicyStatus(Policy policy) throws SQLException {
        // Check previous policy status
        String previousStatus = getPreviousPolicyStatus(policy);
        
        if ("N".equals(previousStatus) || "P".equals(previousStatus)) {
            updatePolicyStatus(policy.getRecno(), "P");
        }
        
        // Additional validations
        if (!validatePolicyRequirements(policy)) {
            updatePolicyStatus(policy.getRecno(), "N");
        }
    }
    
    private boolean validatePolicyRequirements(Policy policy) throws SQLException {
        return hasValidPremium(policy) && 
               hasValidBusinessCode(policy) && 
               hasValidBroker(policy) && 
               hasValidInsured(policy);
    }
    
    private boolean isSIPPolicy(Policy policy) {
        return hs_SgiSip.contains(policy.getBmType());
    }
    
    private void handleSIPPolicy(Policy policy) throws SQLException {
        updatePolicyStatus(policy.getRecno(), "Pf");
    }
    
    // Additional helper methods
    private void updatePolicyStatus(int recno, String status) throws SQLException {
        String sql = "UPDATE edipolicy SET edipolok = ? WHERE edirecno = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, recno);
            ps.executeUpdate();
        }
    }
    
    private void updatePolicyBusinessCode(int recno, String businessCode) throws SQLException {
        String sql = "UPDATE edipolicy SET edibuscode = ? WHERE edirecno = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, businessCode);
            ps.setInt(2, recno);
            ps.executeUpdate();
        }
    }
    
    private String getPreviousPolicyStatus(Policy policy) throws SQLException {
        String sql = "SELECT edipolok FROM edipolicy WHERE edipolno = ? AND edirecno < ? " +
                    "ORDER BY edirecno DESC FETCH FIRST 1 ROWS ONLY";
                    
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, policy.getPreviousPolicyNumber());
            ps.setInt(2, policy.getRecno());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("edipolok");
                }
            }
        }
        return null;
    }
    
    // Validation methods
    private boolean hasValidPremium(Policy policy) {
        return (policy.getBmTermPremium() != null && policy.getBmTermPremium() > 0) ||
               (policy.getBmAnnualPremium() != null && policy.getBmAnnualPremium() > 0) ||
               REVISION.equals(policy.getTransactionCode());
    }
    
    private boolean hasValidBusinessCode(Policy policy) throws SQLException {
        String sql = "SELECT COUNT(*) FROM edibuscod WHERE edibuscode = ? AND edicompany = " +
                    "(SELECT edicompno FROM edipolicy WHERE edirecno = ?)";
                    
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, policy.getBmType());
            ps.setInt(2, policy.getRecno());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }
    
    public static class ProductDeterminationException extends RuntimeException {
        public ProductDeterminationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
