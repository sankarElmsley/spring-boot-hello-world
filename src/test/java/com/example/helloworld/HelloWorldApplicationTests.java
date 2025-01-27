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




import lombok.Data;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.LocalDateTime;
import java.util.*;

@Data
@Builder
@Entity
@Table(name = "EDILOCVAL")
public class EDILocationValue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Integer ediRecNo;
    private Integer locNo;
    private Integer locVal;
    private String locBIForm;
    private Double locBILimit;
    private Double locBIVal;
    private Double locAPrem;
    private Double locPremium;
    private Double locCommission;
    private Double locDeduct;
    private Double locILValue;
    private String locPropType;
    private String locBMCov;
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private Integer createUser;
    private Integer updateUser;
}

public interface LocationValueRepository extends JpaRepository<EDILocationValue, Long> {
    List<EDILocationValue> findByEdiRecNo(Integer ediRecNo);
}

@Service
@Slf4j
@Transactional
public class LocationValueService {


    @Autowired
    private LocationValueRepository locationValueRepository;

    public void processLocationValues(Location location, String recNo) {
        try {
            int locVal = 0;
            List<EDILocationValue> values = location.isHomeowner() ? 
                createHomeownerValues(location, recNo, locVal) :
                createCommercialValues(location, recNo, locVal);

            writeLocValues(values);
            
        } catch (Exception e) {
            log.error("Error processing location values for recNo {}: {}", recNo, e.getMessage());
            throw new LocationValueProcessingException("Failed to process location values", e);
        }
    }

    private List<EDILocationValue> createHomeownerValues(Location location, String recNo, int locVal) {
        List<EDILocationValue> values = new ArrayList<>();
        String bmCover = location.getLocBMCov();

        if (SGIConstants.HSP.equals(bmCover)) {
            values.add(createHSPSLCValue(location, recNo, ++locVal, bmCover, true));
        } else if (SGIConstants.SLC.equals(bmCover)) {
            values.add(createHSPSLCValue(location, recNo, ++locVal, bmCover, false));
        } else if (SGIConstants.HSPSLC.equals(bmCover)) {
            values.add(createHSPSLCValue(location, recNo, ++locVal, bmCover, true));  // HSP
            values.add(createHSPSLCValue(location, recNo, ++locVal, bmCover, false)); // SLC
        }

        return values;
    }

    private List<EDILocationValue> createCommercialValues(Location location, String recNo, int locVal) {
        List<EDILocationValue> values = new ArrayList<>();

        addBIFormValueIfPresent(values, location, recNo, ++locVal, 
            location.getLocBIForm1(), location.getLocBILimit1());
        addBIFormValueIfPresent(values, location, recNo, ++locVal, 
            location.getLocBIForm2(), location.getLocBILimit2());
        addBIFormValueIfPresent(values, location, recNo, ++locVal, 
            location.getLocBIForm3(), location.getLocBILimit3());
        addBIFormValueIfPresent(values, location, recNo, ++locVal, 
            location.getLocBIForm4(), location.getLocBILimit4());
        addBIFormValueIfPresent(values, location, recNo, ++locVal, 
            location.getLocBIForm5(), location.getLocBILimit5());
        addBIFormValueIfPresent(values, location, recNo, ++locVal, 
            location.getLocBIForm6(), location.getLocBILimit6());

        return values;
    }

    private void addBIFormValueIfPresent(List<EDILocationValue> values, Location location, 
            String recNo, int locVal, String biForm, Double biLimit) {
        if (StringUtils.isNotEmpty(biForm) && biLimit != null) {
            values.add(createBIFormValue(location, recNo, locVal, biForm, biLimit, 
                location.getLocBuildingLimit()));
        }
    }

    private EDILocationValue createHSPSLCValue(Location location, String recNo, 
            int locVal, String bmCover, boolean isHSP) {
        
        EDILocationValue.EDILocationValueBuilder builder = EDILocationValue.builder()
            .ediRecNo(Integer.parseInt(recNo))
            .locNo(location.getLocNo())
            .locVal(locVal)
            .locBMCov("0");

        if (isHSP || SGIConstants.HSP.equals(bmCover)) {
            builder
                .locAPrem(location.getLocHSPFTPrem())
                .locPremium(location.getLocHSPPremWrit())
                .locCommission(location.getLocHSPComm())
                .locDeduct(location.getLocHSPDeduct())
                .locILValue(location.getLocHSPLimit())
                .locPropType(SGIConstants.HSP);
        } else {
            builder
                .locAPrem(location.getLocSLCFTPrem())
                .locPremium(location.getLocSLCPremWrit())
                .locCommission(location.getLocSLCComm())
                .locDeduct(location.getLocSLCDeduct())
                .locILValue(location.getLocSLCLimit())
                .locPropType(SGIConstants.SLC);
        }

        return builder.build();
    }

    private EDILocationValue createBIFormValue(Location location, String recNo, 
            int locVal, String biForm, Double biLimit, Double buildingValue) {
        
        return EDILocationValue.builder()
            .ediRecNo(Integer.parseInt(recNo))
            .locNo(location.getLocNo())
            .locVal(locVal)
            .locBIForm(lookupCoverageCodeDesc(biForm))
            .locBILimit(biLimit)
            .locBIVal(buildingValue)
            .locPropType("BI")
            .build();
    }

    @Transactional
    public void writeLocValues(List<EDILocationValue> values) {
        try {
            LocalDateTime now = LocalDateTime.now();
            Integer userId = getCurrentUserId();

            values.forEach(val -> {
                val.setCreateDate(now);
                val.setUpdateDate(now);
                val.setCreateUser(userId);
                val.setUpdateUser(userId);
            });

            locationValueRepository.saveAll(values);
            log.info("Successfully wrote {} location values", values.size());

        } catch (Exception e) {
            log.error("Failed to write location values: {}", e.getMessage());
            throw new LocationValuePersistenceException("Failed to write location values", e);
        }
    }

    private String lookupCoverageCodeDesc(String code) {
        return CoverageCodeMap.get(code);
    }

    private Integer getCurrentUserId() {
        // Implement your user context logic
        return 1;
    }
}

@Component
public class CoverageCodeMap {
    private static final Map<String, String> CODES = Map.ofEntries(
        Map.entry("691", "Blanket Business Interruption Insurance (Gross Earnings Form)"),
        Map.entry("692", "Blanket Business Interruption Insurance (Profits Form)"),
        Map.entry("272", "Blanket Earnings Insurance (No Co-Insurance Form)"),
        Map.entry("313", "Blanket Extra Expense Insurance"),
        Map.entry("561", "Blanket Rent or Rental Value Insurance"),
        Map.entry("127", "Business Interruption (Gross Earnings Form)"),
        Map.entry("126", "Business Interruption (Gross Rentals Form)"),
        Map.entry("128", "Business Interruption (Profits Form)")
    );

    public static String get(String code) {
        return Optional.ofNullable(CODES.get(code)).orElse(code);
    }
}

public class LocationValueProcessingException extends RuntimeException {
    public LocationValueProcessingException(String message) {
        super(message);
    }

    public LocationValueProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}

public class LocationValuePersistenceException extends RuntimeException {
    public LocationValuePersistenceException(String message) {
        super(message);
    }

    public LocationValuePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }
}
