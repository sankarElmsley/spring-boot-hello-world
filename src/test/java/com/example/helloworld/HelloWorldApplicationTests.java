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
public class PolicyBmTypeMapper {

    @Autowired 
    private JdbcTemplate jdbcTemplate;

    /**
     * Gets previous policy BM types for policies with specific transaction codes
     * @param policies List of policies to process
     * @return Map of policy number to BM type info
     */
    public Map<String, String> getPreviousBmTypes(List<Policy> policies) {
        List<String> filteredPolNos = policies.stream()
            .filter(policy -> Arrays.asList("2", "4").contains(policy.getPolicyData().getEditrncode()))
            .map(Policy::getPolNo)
            .collect(Collectors.toList());

        if (CollectionUtils.isEmpty(filteredPolNos)) {
            log.info("No policies found with transaction codes 2 or 4");
            return Collections.emptyMap();
        }

        Map<String, String> bmTypeByPolNo = new HashMap<>();
        List<Map<String, Object>> bmTypeList = new ArrayList<>();
        
        // Process in batches of 500
        ListUtils.partition(filteredPolNos, 500).forEach(polNoList -> 
            findPreviousBmTypesForBatch(polNoList, bmTypeList));

        if (CollectionUtils.isNotEmpty(bmTypeList)) {
            bmTypeByPolNo = bmTypeList.stream()
                .collect(Collectors.toMap(
                    row -> ((String) row.get("edipolno")).trim(),
                    row -> ((String) row.get("edibmtype")).trim(),
                    (existing, replacement) -> existing
                ));
        }

        return bmTypeByPolNo;
    }

    private void findPreviousBmTypesForBatch(List<String> polNos, List<Map<String, Object>> bmTypeList) {
        String params = StringUtils.repeat(",?", polNos.size()).substring(1);
        String sqlQuery = 
            "SELECT distinct TRIM(edipolno) as edipolno, TRIM(edibmtype) as edibmtype " +
            "FROM edipolicy WHERE edirecno in (" +
            "SELECT MAX(edirecno) FROM edipolicy ep GROUP BY edipolno, edipololdno " +
            "HAVING (ep.edipolno in (" + params + ") OR ep.edipololdno in (" + params + ")))";

        try {
            Object[] paramVals = ArrayUtils.addAll(polNos.toArray(), polNos.toArray());
            int[] paramTypes = new int[polNos.size() * 2];
            Arrays.fill(paramTypes, Types.VARCHAR);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sqlQuery, paramVals, paramTypes);
            if (CollectionUtils.isNotEmpty(results)) {
                bmTypeList.addAll(results);
            }
        } catch (Exception e) {
            log.error("Error finding previous BM types for policies: {}", polNos, e);
            throw new BmTypeMappingException("Failed to find previous BM types", e);
        }
    }

    /**
     * Apply previous BM type mappings to policies
     */
    public void applyPreviousBmTypes(List<Policy> policies) {
        Map<String, String> bmTypeMap = getPreviousBmTypes(policies);
        
        policies.forEach(policy -> {
            if (Arrays.asList("2", "4").contains(policy.getPolicyData().getEditrncode())) {
                String prevBmType = bmTypeMap.get(policy.getPolNo());
                
                if (StringUtils.isEmpty(prevBmType)) {
                    policy.setStatus(DataConstants.TRANSACTION_STATUS_ERROR);
                    policy.addError("No previous BM type found");
                    log.warn("No previous BM type for policy: {}", policy.getPolNo());
                } else if (homeowners.equalsIgnoreCase(prevBmType)) {
                    policy.setStatus(DataConstants.TRANSACTION_STATUS_ERROR);
                    policy.addError("The bmtype for the previous record was not properly set!");
                    log.warn("Previous BM type was HOMEOWNERS for policy: {}", policy.getPolNo());
                } else if (HSP.equals(prevBmType) || SLC.equals(prevBmType) || HSPSLC.equals(prevBmType)) {
                    policy.getPolicyData().setEdibmtype(prevBmType);
                    log.info("Updated BM type for policy {} to {}", policy.getPolNo(), prevBmType);
                } else {
                    policy.setStatus(DataConstants.TRANSACTION_STATUS_ERROR);
                    policy.addError("The bmtype for the previous record was wrong!");
                    log.warn("Invalid previous BM type {} for policy: {}", prevBmType, policy.getPolNo());
                }
            }
        });
    }
}





@Service
@Slf4j
public class LocationValueFinder {
    
    public void updatePolicyWithLocationCode(EDIPolicy policy, List<EDILocation> locations) {
        if (CollectionUtils.isEmpty(locations)) {
            log.warn("No locations provided for policy {}", policy.getEDIRECNO());
            return;
        }

        EDILocation locationToUse;
        if (locations.size() == 1) {
            locationToUse = locations.get(0);
        } else {
            // This part maps to the subquery that finds max value:
            // SELECT max(CASE WHEN edilocilvalue IS NULL THEN (edilocbvalue + ediloccvalue) ELSE edilocilvalue END)
            locationToUse = locations.stream()
                .max((loc1, loc2) -> {
                    double value1 = calculateLocationValue(loc1);  // Maps to the CASE statement
                    double value2 = calculateLocationValue(loc2);
                    return Double.compare(value1, value2);
                })
                .orElse(null);
        }

        // This maps to the final SELECT of business code fields
        if (locationToUse != null) {
            policy.setEDIBUSCODE(locationToUse.getEDILOCBUSCODE());
            policy.setEDIBUSSUB(0);
        }
    }

    // This method maps to the CASE statement in both the subquery and main query:
    // CASE WHEN edilocilvalue IS NULL THEN (edilocbvalue + ediloccvalue) ELSE edilocilvalue END
    private double calculateLocationValue(EDILocation location) {
        if (location.getEDILOCILVALUE() != null) {            // WHEN edilocilvalue IS NULL
            return location.getEDILOCILVALUE();               // ELSE edilocilvalue
        }
        
        double buildingValue = location.getEDILOCBVALUE() != null ? location.getEDILOCBVALUE() : 0.0;
        double contentsValue = location.getEDILOCCVALUE() != null ? location.getEDILOCCVALUE() : 0.0;
        
        return buildingValue + contentsValue;                  // THEN (edilocbvalue + ediloccvalue)
    }
}



--------------------------------------------------------------------

    @Service
@Slf4j
public class LocationValueFinder {
    
    public void updatePolicyAndLocationsWithBusinessCode(EDIPolicy policy, List<EDILocation> locations) {
        if (CollectionUtils.isEmpty(locations)) {
            log.warn("No locations provided for policy {}", policy.getEDIRECNO());
            return;
        }

        // Find location with max value
        EDILocation locationWithMaxValue = locations.size() == 1 ? 
            locations.get(0) : 
            locations.stream()
                .max((loc1, loc2) -> Double.compare(
                    calculateLocationValue(loc1), 
                    calculateLocationValue(loc2)))
                .orElse(null);

        if (locationWithMaxValue != null) {
            String businessCode = locationWithMaxValue.getEDILOCBUSCODE();
            
            // Update policy with business code from max value location
            policy.setEDIBUSCODE(businessCode);
            policy.setEDIBUSSUB(0);
            
            // Update all locations that have missing/invalid business code
            locations.stream()
                .filter(location -> StringUtils.isEmpty(location.getEDILOCBUSCODE()))
                .forEach(location -> {
                    location.setEDILOCBUSCODE(businessCode);
                    location.setEDILOCBUSSUB(0);
                    log.info("Updated location {} with business code {} from max value location {}", 
                        location.getEDILOCNO(),
                        businessCode,
                        locationWithMaxValue.getEDILOCNO());
                });

            log.info("Updated policy {} and its locations with business code {} from location {}", 
                policy.getEDIRECNO(), 
                businessCode,
                locationWithMaxValue.getEDILOCNO());
        }
    }

    private double calculateLocationValue(EDILocation location) {
        if (location.getEDILOCILVALUE() != null) {
            return location.getEDILOCILVALUE();
        }
        
        double buildingValue = location.getEDILOCBVALUE() != null ? location.getEDILOCBVALUE() : 0.0;
        double contentsValue = location.getEDILOCCVALUE() != null ? location.getEDILOCCVALUE() : 0.0;
        
        return buildingValue + contentsValue;
    }
}
++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++



    /**
 * Models for the domain objects
 */
@Data
class Policy {
    private String recno;
    private String trnCode;
    private String companyNo;
    private List<Location> locations;
}

@Data
class Location {
    private String businessCode;
    private double limitValue;
}

@Data
class BusinessCode {
    private String code;
    private String companyNo;
}

/**
 * Service to check policy validity regarding locations and business codes.
 * Returns 1 if valid, 0 if invalid.
 */
public class PolicyValidator {
    private final Logger log = LoggerFactory.getLogger(PolicyValidator.class);
    
    /**
     * Checks if a policy has valid locations and business codes.
     * For transaction codes 1 (New) or 3 (Renewal):
     * - Verifies that policy has at least one location
     * - Verifies that all locations have valid business codes for the company
     * 
     * @param policy The policy to validate
     * @param validBusinessCodes List of valid business codes for the company
     * @return 1 if validation passes, 0 if validation fails
     */
    public int ebrPolicyCheck(Policy policy, List<BusinessCode> validBusinessCodes) {
        try {
            // Only validate for new business (1) or renewal (3)
            if (!isTransactionValidForCheck(policy.getTrnCode())) {
                return 1; // Pass validation for other transaction types
            }

            // Check if policy has locations
            if (!hasLocations(policy)) {
                logError(policy.getRecno(), "Missing Locations");
                setHavingLocation(false);
                return 0;
            }

            setHavingLocation(true);

            // Check if all locations have valid business codes
            if (!hasValidBusinessCodes(policy, validBusinessCodes)) {
                logError(policy.getRecno(), "Missing/Invalid Location Business Codes");
                return 0;
            }

            return 1; // All validations passed

        } catch (Exception e) {
            log.error("Error validating policy {}: {}", policy.getRecno(), e.getMessage());
            return 0;
        }
    }

    private boolean isTransactionValidForCheck(String trnCode) {
        return "1".equals(trnCode) || "3".equals(trnCode);
    }

    private boolean hasLocations(Policy policy) {
        return policy.getLocations() != null && !policy.getLocations().isEmpty();
    }

    private boolean hasValidBusinessCodes(Policy policy, List<BusinessCode> validBusinessCodes) {
        Set<String> validCodes = validBusinessCodes.stream()
            .filter(bc -> bc.getCompanyNo().equals(policy.getCompanyNo()))
            .map(BusinessCode::getCode)
            .collect(Collectors.toSet());

        return policy.getLocations().stream()
            .allMatch(location -> isValidBusinessCode(location, validCodes));
    }

    private boolean isValidBusinessCode(Location location, Set<String> validCodes) {
        String businessCode = location.getBusinessCode();
        return businessCode != null && validCodes.contains(businessCode);
    }

    private void logError(String recno, String message) {
        log.error("Policy {}: {}", recno, message);
        // Call your existing error logging mechanism
        // ediLogUpdate(recno, "E", message);
    }

    private boolean isHavingLocation;

    /**
     * Sets whether the policy has locations
     * @param value true if policy has locations, false otherwise
     */
    private void setHavingLocation(boolean value) {
        this.isHavingLocation = value;
    }

    /**
     * Gets whether the policy has locations
     * @return true if policy has locations, false otherwise
     */
    public boolean isHavingLocation() {
        return this.isHavingLocation;
    }
}

/**
 * Example usage
 */
class Example {
    void demonstrateUsage() {
        // Create sample data
        Policy policy = new Policy();
        policy.setRecno("12345");
        policy.setTrnCode("1");  // New business
        policy.setCompanyNo("19");
        policy.setLocations(Arrays.asList(
            createLocation("BC001", 100000.0),
            createLocation("BC002", 200000.0)
        ));

        List<BusinessCode> validBusinessCodes = Arrays.asList(
            createBusinessCode("BC001", "19"),
            createBusinessCode("BC002", "19")
        );

        // Validate policy
        PolicyValidator validator = new PolicyValidator();
        int result = validator.ebrPolicyCheck(policy, validBusinessCodes);
        
        if (result == 1) {
            System.out.println("Policy validation passed");
        } else {
            System.out.println("Policy validation failed");
        }
    }

    private Location createLocation(String businessCode, double limitValue) {
        Location location = new Location();
        location.setBusinessCode(businessCode);
        location.setLimitValue(limitValue);
        return location;
    }

    private BusinessCode createBusinessCode(String code, String companyNo) {
        BusinessCode businessCode = new BusinessCode();
        businessCode.setCode(code);
        businessCode.setCompanyNo(companyNo);
        return businessCode;
    }
}
