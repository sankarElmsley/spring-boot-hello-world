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
import java.util.HashMap;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProductMapper {
    
    private final Connection connection;
    private final Map<String, String> productMappings;
    
    @Data
    @Builder
    public static class Policy {
        private String policyNumber;
        private String bmType;
        private String bmTypeCov;
        private int recno;
        private String packType;
    }
    
    public ProductMapper(Connection connection, Map<String, String> existingProductMappings) {
        this.connection = connection;
        this.productMappings = existingProductMappings;
    }
    
    /**
     * Maps a policy to its product ID using pre-loaded mappings and business rules
     */
    public String mapProduct(Policy policy) {
        String productId = "";
        
        try {
            // First check if we have a direct mapping
            if (policy.getBmType() != null) {
                productId = productMappings.get(policy.getBmType());
            }
            
            // If no direct mapping found, apply business rules
            if (productId == null || productId.isEmpty()) {
                productId = determineProductByBusinessRules(policy);
            }
            
            log.info("Mapped policy {} to product {}", policy.getPolicyNumber(), productId);
            
        } catch (Exception e) {
            log.error("Product mapping failed for policy: {}", policy.getPolicyNumber(), e);
            throw new ProductMappingException("Error mapping product", e);
        }
        
        return productId;
    }
    
    /**
     * Determines product ID based on business rules
     */
    private String determineProductByBusinessRules(Policy policy) {
        // Handle B or U type policies per CR#1538
        if ("B".equals(policy.getBmType()) || "U".equals(policy.getBmType())) {
            return "R1400";
        }
        
        // Handle S type policies
        if ("S".equals(policy.getBmTypeCov())) {
            return "RTF18";
        }
        
        // Complex type determination based on bmTypeCov and bmType combination
        if (policy.getBmTypeCov() != null && policy.getBmType() != null) {
            String combinedType = policy.getBmTypeCov() + "/" + policy.getBmType();
            
            // Check invalid combinations
            if ("U/F-3".equals(combinedType)) {
                throw new ProductMappingException(
                    "Invalid Type Coverage PolBMTypeCov/PolBMType combination: " + combinedType,
                    null
                );
            }
            
            // Check if we have a mapping for the combined type
            String mappedProduct = productMappings.get(combinedType);
            if (mappedProduct != null) {
                return mappedProduct;
            }
        }
        
        // Check for cyber product types (based on your specific business rules)
        if (isCyberProduct(policy.getBmType())) {
            return determineCyberProductType(policy);
        }
        
        // Default product type if no other rules match
        return "RTF18";
    }
    
    private boolean isCyberProduct(String bmType) {
        if (bmType == null) return false;
        return bmType.contains("DC1") || bmType.contains("DC3") || 
               bmType.contains("C1") || bmType.contains("C3");
    }
    
    private String determineCyberProductType(Policy policy) {
        String bmType = policy.getBmType();
        
        // Combined product types
        if (bmType.contains("DC1") && bmType.contains("DC3")) {
            return "RTF321"; // Example - adjust according to your rules
        }
        
        if (bmType.contains("C1") && bmType.contains("C3")) {
            return "RTF322"; // Example - adjust according to your rules
        }
        
        // Single product types
        if (bmType.contains("DC1") || bmType.contains("DC3")) {
            return "RTF323"; // Example - adjust according to your rules
        }
        
        return "RTF18"; // Default fallback
    }
    
    /**
     * Updates the product mappings cache
     */
    public void updateProductMappings(Map<String, String> newMappings) {
        productMappings.clear();
        productMappings.putAll(newMappings);
    }
    
    /**
     * Custom exception for product mapping errors
     */
    public static class ProductMappingException extends RuntimeException {
        public ProductMappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    
    /**
     * Example usage showing how to create and use the mapper
     */
    public static void main(String[] args) {
        // Example pre-loaded product mappings
        Map<String, String> existingMappings = new HashMap<>();
        existingMappings.put("B", "R1400");
        existingMappings.put("U", "R1400");
        existingMappings.put("S/F-3", "RTF18");
        
        // Create mapper with existing mappings
        ProductMapper mapper = new ProductMapper(connection, existingMappings);
        
        // Create a policy object
        Policy policy = Policy.builder()
            .policyNumber("POL123")
            .bmType("B")
            .bmTypeCov("S")
            .recno(1)
            .packType("40")
            .build();
            
        // Get product mapping
        String productId = mapper.mapProduct(policy);
        System.out.println("Mapped Product ID: " + productId);
    }
}
