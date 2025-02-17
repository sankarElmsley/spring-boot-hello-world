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
public class ProductMapper {

    @Autowired 
    private JdbcTemplate jdbcTemplate;

    private Map<String, String> bmTypeProductMap;

    @PostConstruct
    public void initializeBmTypeMap() {
        loadBmTypeProductMap();
    }

    private void loadBmTypeProductMap() {
        String sql = "select edibmtype, prdtp from ediproduct where edicompno in (17,44)";
        
        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            bmTypeProductMap = results.stream()
                .collect(Collectors.toMap(
                    row -> ((String) row.get("edibmtype")).trim(),
                    row -> ((String) row.get("prdtp")).trim(),
                    (existing, replacement) -> existing
                ));
            
            log.info("Loaded {} BM type mappings", bmTypeProductMap.size());
        } catch (Exception e) {
            log.error("Error loading BM type product mappings", e);
            throw new ProductMappingException("Failed to load BM type mappings", e);
        }
    }

    public void mapProducts(List<Policy> policies) {
        policies.forEach(policy -> {
            String bmType = policy.getPolicyData().getEdibmtype();
            if (bmType != null) {
                String productId = bmTypeProductMap.get(bmType.trim());
                if (StringUtils.isEmpty(productId)) {
                    policy.setStatus(DataConstants.TRANSACTION_STATUS_ERROR);
                    policy.addError("Missing/Incorrect BM Type");
                    log.warn("No product mapping found for BM type: {}", bmType);
                }
                policy.setProductId(productId);
            } else {
                policy.setStatus(DataConstants.TRANSACTION_STATUS_ERROR);
                policy.addError("Missing BM Type");
                log.warn("Null BM type for policy: {}", policy.getPolNo());
            }
        });
    }

    // Method to refresh the mapping if needed
    public void refreshBmTypeMap() {
        loadBmTypeProductMap();
    }
}




@Service
@Slf4j
public class ProductMapper {

    @Autowired 
    private JdbcTemplate jdbcTemplate;

    private Map<String, String> bmTypeProductMap;

    /**
     * Gets BM type to product mappings for given company numbers
     * @param companyNumbers List of company numbers
     * @return Map of BM type to product ID mappings
     */
    public Map<String, String> getBmTypeProductMap(List<Integer> companyNumbers) {
        if (CollectionUtils.isEmpty(companyNumbers)) {
            log.warn("No company numbers provided for BM type mapping");
            return Collections.emptyMap();
        }

        String params = StringUtils.repeat(",?", companyNumbers.size()).substring(1);
        String sql = "select edibmtype, prdtp from ediproduct where edicompno in (" + params + ")";

        try {
            List<Map<String, Object>> results = jdbcTemplate.queryForList(
                sql,
                companyNumbers.toArray(),
                generateParamTypes(companyNumbers.size())
            );

            Map<String, String> bmTypeMap = results.stream()
                .collect(Collectors.toMap(
                    row -> ((String) row.get("edibmtype")).trim(),
                    row -> ((String) row.get("prdtp")).trim(),
                    (existing, replacement) -> existing
                ));
            
            log.info("Loaded {} BM type mappings for companies: {}", 
                bmTypeMap.size(), companyNumbers);
            
            return bmTypeMap;

        } catch (Exception e) {
            log.error("Error loading BM type product mappings for companies: {}", 
                companyNumbers, e);
            throw new ProductMappingException(
                "Failed to load BM type mappings for companies: " + companyNumbers, e);
        }
    }

    private int[] generateParamTypes(int size) {
        int[] paramTypes = new int[size];
        Arrays.fill(paramTypes, Types.INTEGER);
        return paramTypes;
    }

    /**
     * Maps products for a list of policies using provided company numbers
     * @param policies List of policies to process
     * @param companyNumbers List of company numbers to get mappings for
     */
    public void mapProducts(List<Policy> policies, List<Integer> companyNumbers) {
        // Get fresh mapping for requested companies
        Map<String, String> bmTypeMap = getBmTypeProductMap(companyNumbers);
        
        if (bmTypeMap.isEmpty()) {
            log.error("No BM type mappings found for companies: {}", companyNumbers);
            policies.forEach(policy -> {
                policy.setStatus(DataConstants.TRANSACTION_STATUS_ERROR);
                policy.addError("No BM type mappings available");
            });
            return;
        }

        policies.forEach(policy -> {
            String bmType = policy.getPolicyData().getEdibmtype();
            if (bmType != null) {
                String productId = bmTypeMap.get(bmType.trim());
                if (StringUtils.isEmpty(productId)) {
                    policy.setStatus(DataConstants.TRANSACTION_STATUS_ERROR);
                    policy.addError("Missing/Incorrect BM Type");
                    log.warn("No product mapping found for BM type: {} in companies: {}", 
                        bmType, companyNumbers);
                }
                policy.setProductId(productId);
            } else {
                policy.setStatus(DataConstants.TRANSACTION_STATUS_ERROR);
                policy.addError("Missing BM Type");
                log.warn("Null BM type for policy: {}", policy.getPolNo());
            }
        });
    }
}
