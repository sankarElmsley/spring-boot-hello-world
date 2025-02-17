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
