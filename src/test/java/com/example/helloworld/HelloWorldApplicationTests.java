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



import java.util.List;
import java.util.Optional;
import java.util.Comparator;

@Service
public class DeductibleUpdateService {
    
    private final EdiPolicyRepository policyRepository;
    private final EdiLocationRepository locationRepository;

    @Autowired
    public DeductibleUpdateService(EdiPolicyRepository policyRepository, 
                                 EdiLocationRepository locationRepository) {
        this.policyRepository = policyRepository;
        this.locationRepository = locationRepository;
    }

    @Transactional
    public void updatePolicyDeductibleFromHighestLocation(String recNo) {
        // Find policies with zero or null deductible
        Optional<EdiPolicy> policyOpt = policyRepository.findByRecNoAndDeductibleZeroOrNull(recNo);
        
        if (policyOpt.isPresent()) {
            // Get all locations for this policy
            List<EdiLocation> locations = locationRepository.findByRecNo(recNo);
            
            // Find highest deductible using Java 8 streams
            Optional<Double> highestDeductible = locations.stream()
                .map(EdiLocation::getDeductible)
                .filter(deductible -> deductible != null)
                .max(Comparator.naturalOrder());
                
            // Update policy if highest deductible found
            highestDeductible.ifPresent(deductible -> {
                EdiPolicy policy = policyOpt.get();
                policy.setDeductible(deductible);
                policyRepository.save(policy);
            });
        }
    }
}

@Repository
public interface EdiPolicyRepository extends JpaRepository<EdiPolicy, Long> {
    @Query("SELECT p FROM EdiPolicy p WHERE p.recNo = :recNo " +
           "AND (p.deductible = 0 OR p.deductible IS NULL)")
    Optional<EdiPolicy> findByRecNoAndDeductibleZeroOrNull(@Param("recNo") String recNo);
}

@Repository
public interface EdiLocationRepository extends JpaRepository<EdiLocation, Long> {
    List<EdiLocation> findByRecNo(String recNo);
}

@Entity
@Table(name = "edipolicy")
public class EdiPolicy {
    @Id
    private Long id;
    
    @Column(name = "edirecno")
    private String recNo;
    
    @Column(name = "edibmdeduct")
    private Double deductible;
    
    // Getters and setters
}

@Entity
@Table(name = "edilocation")
public class EdiLocation {
    @Id
    private Long id;
    
    @Column(name = "edirecno")
    private String recNo;
    
    @Column(name = "edilocdeduct")
    private Double deductible;
    
    // Getters and setters
}
