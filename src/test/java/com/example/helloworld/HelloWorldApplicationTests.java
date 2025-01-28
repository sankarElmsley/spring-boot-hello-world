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
 * Model for Policy Value
 */
@Data
@Builder
public class PolicyValue {
    private String pvLineNo;
    private String pvCoverage;
    private String pvTpremium;
    private String pvAPremium;
    private String pvLimit;
    private String pvDeduct;
    private String pvCommission;
}

/**
 * Service to process policy values and determine coverage types
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModernPolicyValueProcessor {
    
    private String combinedProdType = "";
    private boolean isOptAndDc = false;

    /**
     * Process list of PolicyValue objects to check coverage types
     */
    public void checkProductInCoverageSection(List<PolicyValue> policyValues) {
        log.debug("Analyzing {} policy values for coverage types", policyValues.size());
        
        // Reset state
        combinedProdType = "";
        isOptAndDc = false;

        // Process coverages
        Map<String, String> coverageMap = policyValues.stream()
            .map(PolicyValue::getPvCoverage)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(
                this::mapCoverageTypeCode,
                coverage -> coverage,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));

        // If coverages found, build combined type
        if (!coverageMap.isEmpty()) {
            combinedProdType = coverageMap.keySet().stream()
                .filter(key -> !key.equals("UNKNOWN"))
                .sorted()
                .collect(Collectors.joining());
            isOptAndDc = true;

            log.info("Found cyber coverages: {}, Combined type: {}", 
                    coverageMap.values(), combinedProdType);
        } else {
            log.info("No cyber coverages found in policy values");
        }
    }

    /**
     * Map coverage description to type code
     */
    private String mapCoverageTypeCode(String coverage) {
        if (coverage.contains("RespEx")) return "DC1";
        if (coverage.contains("DefLia")) return "DC3";
        if (coverage.contains("ComAtt")) return "C1";
        if (coverage.contains("NetLia")) return "C3";
        return "UNKNOWN";
    }

    /**
     * Get policy type based on coverage combinations
     */
    public PolicyType determinePolicyType() {
        if (!isOptAndDc) {
            return PolicyType.STANDARD;
        }

        if (combinedProdType.length() > 3 && 
            !combinedProdType.equals("DC1DC3") && 
            !combinedProdType.equals("C1C3")) {
            return PolicyType.COMBINED_DC_CF;
        } else if (combinedProdType.matches("DC[13]|DC1DC3")) {
            return PolicyType.DATA_COMPROMISE;
        } else if (combinedProdType.matches("C[13]|C1C3")) {
            return PolicyType.CYBER_FIRST;
        }
        
        return PolicyType.STANDARD;
    }

    /**
     * Get suffix based on policy type
     */
    public String determinePolicySuffix() {
        return switch (determinePolicyType()) {
            case COMBINED_DC_CF -> "-Z";
            case DATA_COMPROMISE -> "-D";
            case CYBER_FIRST -> "-Y";
            default -> "";
        };
    }

    public String getCombinedProdType() {
        return combinedProdType;
    }

    public boolean isOptAndDc() {
        return isOptAndDc;
    }
}

/**
 * Enum for policy types
 */
public enum PolicyType {
    STANDARD,
    DATA_COMPROMISE,
    CYBER_FIRST,
    COMBINED_DC_CF
}

/**
 * Example usage in main processing class
 */
@Slf4j
public class PolicyProcessor {
    
    private final ModernPolicyValueProcessor policyValueProcessor;
    
    public void processPolicy(List<PolicyValue> policyValues) {
        // Analyze coverages
        policyValueProcessor.checkProductInCoverageSection(policyValues);
        
        // If cyber coverages found
        if (policyValueProcessor.isOptAndDc()) {
            PolicyType policyType = policyValueProcessor.determinePolicyType();
            String suffix = policyValueProcessor.determinePolicySuffix();
            
            log.info("Processing split policy - Type: {}, Suffix: {}, Combined Type: {}", 
                    policyType, 
                    suffix,
                    policyValueProcessor.getCombinedProdType());

            // Handle split policy based on type...
            processSplitPolicy(policyValues, policyType, suffix);
        } else {
            log.info("Processing standard policy");
            processStandardPolicy(policyValues);
        }
    }

    private void processSplitPolicy(List<PolicyValue> policyValues, PolicyType type, String suffix) {
        // Implementation for split policy processing
    }

    private void processStandardPolicy(List<PolicyValue> policyValues) {
        // Implementation for standard policy processing
    }
}

// Example test data and usage
public class Example {
    public static void main(String[] args) {
        List<PolicyValue> policyValues = Arrays.asList(
            PolicyValue.builder()
                .pvLineNo("PV001")
                .pvCoverage("RespEx")
                .pvTpremium("45")
                .pvAPremium("45")
                .pvLimit("25000")
                .pvDeduct("1000")
                .pvCommission("30.0000")
                .build(),
            PolicyValue.builder()
                .pvLineNo("PV002")
                .pvCoverage("DefLia")
                .pvTpremium("51")
                .pvAPremium("51")
                .pvLimit("25000")
                .pvDeduct("1000")
                .pvCommission("30.0000")
                .build()
            // ... more policy values
        );

        ModernPolicyValueProcessor processor = new ModernPolicyValueProcessor();
        processor.checkProductInCoverageSection(policyValues);
        
        if (processor.isOptAndDc()) {
            String suffix = processor.determinePolicySuffix();
            PolicyType type = processor.determinePolicyType();
            System.out.println("Policy Type: " + type);
            System.out.println("Suffix: " + suffix);
            System.out.println("Combined Type: " + processor.getCombinedProdType());
        }
    }
}
