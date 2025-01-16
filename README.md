import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Complete Location model covering both homeowner and non-homeowner fields
 */
@Data
@Builder
public class Location {
    // Common fields
    private Integer ediRecNo;
    private Integer locNo;
    private String locName;
    private String locAddressType;
    private String locParcel;
    private String locLot;
    private String locBlock;
    private String locPlan;
    private String locQuarter;
    private String locSection;
    private String locTownship;
    private String locRange;
    private String locMeridian;
    private String locCivSuiteNo;
    private String locCivStreetNo;
    private String locCivStreetName;
    private String locStreetCode;
    private String locStreetDirection;
    private String locLocationDesc;
    private String locCity;
    private String locProv;
    private String locPostCode;
    private String locNearInd;
    private String locNearLocName;
    private String locWithinLocName;
    private String locBMChgCd;
    
    // Homeowner specific fields (HSP/SLC)
    private Double locHSPFTPrem;
    private Double locHSPPremWrit;
    private Double locHSPComm;
    private Double locHSPDeduct;
    private Double locHSPLimit;
    private Double locSLCFTPrem;
    private Double locSLCPremWrit;
    private Double locSLCComm;
    private Double locSLCDeduct;
    private Double locSLCLimit;
    private String locBMCov;
    
    // Non-homeowner specific fields
    private String locBusCode;
    private Double locBuildingLimit;
    private Double locDeduct;
    private Double locContentsLimit;
    private Double locContentsDeduct;
    private Double polCONLimit;
    private String locBIForm1;
    private Double locBILimit1;
    private String locBIForm2;
    private Double locBILimit2;
    private String locBIForm3;
    private Double locBILimit3;
    private String locBIForm4;
    private Double locBILimit4;
    private String locBIForm5;
    private Double locBILimit5;
    private String locBIForm6;
    private Double locBILimit6;
    
    // Audit fields
    private LocalDateTime createDate;
    private LocalDateTime updateDate;
    private Integer createUser;
    private Integer updateUser;
}

/**
 * Enum to define location types
 */
public enum LocationType {
    HOMEOWNER,
    NON_HOMEOWNER
}

/**
 * Enhanced builder with complete field mapping
 */
@Builder
public class LocationBuilder {
    private static final int HOMEOWNER_FIELDS = 33;
    private static final int NON_HOMEOWNER_FIELDS = 44;
    private static final String HSP = "HSP";
    private static final String SLC = "SLC";
    private static final String HSPSLC = "HSP/SLC";
    
    private final List<String> tokens;
    private final LocationType locationType;
    private final LocalDateTime currentTime;
    private final Integer userId;

    public Optional<Location> buildLocation() {
        try {
            validateTokens();
            Location.LocationBuilder builder = initializeCommonFields();
            
            if (locationType == LocationType.HOMEOWNER) {
                return Optional.of(buildHomeownerLocation(builder));
            } else {
                return Optional.of(buildNonHomeownerLocation(builder));
            }
        } catch (Exception e) {
            log.error("Failed to build location: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private void validateTokens() {
        int expectedSize = (locationType == LocationType.HOMEOWNER) ? HOMEOWNER_FIELDS : NON_HOMEOWNER_FIELDS;
        if (tokens.size() != expectedSize) {
            throw new IllegalArgumentException(
                String.format("Expected %d tokens but got %d for %s", 
                    expectedSize, tokens.size(), locationType)
            );
        }
    }

    private Location.LocationBuilder initializeCommonFields() {
        return Location.builder()
            .createDate(currentTime)
            .updateDate(currentTime)
            .createUser(userId)
            .updateUser(userId)
            .locNo(parseInteger(tokens.get(0)))
            .locName("Building #" + tokens.get(1))
            .locAddressType(tokens.get(2))
            .locParcel(tokens.get(3))
            .locLot(tokens.get(4))
            .locBlock(tokens.get(5))
            .locPlan(tokens.get(6))
            .locQuarter(tokens.get(7))
            .locSection(tokens.get(8))
            .locTownship(tokens.get(9))
            .locRange(tokens.get(10))
            .locMeridian(tokens.get(11))
            .locCivSuiteNo(tokens.get(12))
            .locCivStreetNo(tokens.get(13))
            .locCivStreetName(tokens.get(14))
            .locStreetCode(tokens.get(15))
            .locStreetDirection(tokens.get(16))
            .locLocationDesc(tokens.get(17))
            .locCity(nullIfDefault(tokens.get(18)))
            .locProv(nullIfDefault(tokens.get(19)))
            .locPostCode(formatPostalCode(tokens.get(20)))
            .locNearInd(tokens.get(21))
            .locNearLocName(tokens.get(22))
            .locWithinLocName(tokens.get(23));
    }

    private Location buildHomeownerLocation(Location.LocationBuilder builder) {
        // Set homeowner specific fields
        builder
            .locBMChgCd(tokens.get(24))
            .locHSPFTPrem(parseDouble(tokens.get(25)))
            .locHSPPremWrit(parseDouble(tokens.get(26)))
            .locHSPComm(parseDouble(tokens.get(27)))
            .locHSPDeduct(parseDouble(tokens.get(28)))
            .locSLCFTPrem(parseDouble(tokens.get(29)))
            .locSLCPremWrit(parseDouble(tokens.get(30)))
            .locSLCComm(parseDouble(tokens.get(31)))
            .locSLCDeduct(parseDouble(tokens.get(32)));

        // Set coverage type based on premium values
        if (hasValue(tokens.get(25)) && hasValue(tokens.get(29))) {
            builder.locBMCov(HSPSLC);
        } else if (hasValue(tokens.get(25))) {
            builder.locBMCov(HSP);
        } else if (hasValue(tokens.get(29))) {
            builder.locBMCov(SLC);
        }

        return builder.build();
    }

    private Location buildNonHomeownerLocation(Location.LocationBuilder builder) {
        return builder
            .locBusCode(tokens.get(24))
            .locBMCov(tokens.get(25))
            .locBMChgCd(tokens.get(26))
            .locBuildingLimit(parseDouble(tokens.get(27)))
            .locDeduct(parseDouble(tokens.get(28)))
            .locContentsLimit(parseDouble(tokens.get(29)))
            .locContentsDeduct(parseDouble(tokens.get(30)))
            .polCONLimit(parseDouble(tokens.get(31)))
            .locBIForm1(tokens.get(32))
            .locBILimit1(parseDouble(tokens.get(33)))
            .locBIForm2(tokens.get(34))
            .locBILimit2(parseDouble(tokens.get(35)))
            .locBIForm3(tokens.get(36))
            .locBILimit3(parseDouble(tokens.get(37)))
            .locBIForm4(tokens.get(38))
            .locBILimit4(parseDouble(tokens.get(39)))
            .locBIForm5(tokens.get(40))
            .locBILimit5(parseDouble(tokens.get(41)))
            .locBIForm6(tokens.get(42))
            .locBILimit6(parseDouble(tokens.get(43)))
            .build();
    }

    private boolean hasValue(String value) {
        return value != null && !value.equals("NULL") && !value.trim().isEmpty();
    }

    private String formatPostalCode(String postalCode) {
        return Optional.ofNullable(postalCode)
            .filter(pc -> !pc.equals("NULL"))
            .map(pc -> pc.replaceAll("\\s+", ""))
            .orElse(null);
    }

    private Double parseDouble(String value) {
        return Optional.ofNullable(value)
            .filter(v -> !v.equals("NULL"))
            .map(Double::parseDouble)
            .orElse(0.0);
    }

    private Integer parseInteger(String value) {
        return Optional.ofNullable(value)
            .filter(v -> !v.equals("NULL"))
            .map(Integer::parseInt)
            .orElse(0);
    }

    private String nullIfDefault(String value) {
        return Optional.ofNullable(value)
            .filter(v -> !v.equals("NULL"))
            .orElse(null);
    }
}

/**
 * Service to handle location processing with transaction management
 */
@Service
@Transactional
@Slf4j
public class LocationService {
    private final LocationRepository locationRepository;
    
    @Autowired
    public LocationService(LocationRepository locationRepository) {
        this.locationRepository = locationRepository;
    }

    public Location processLocationLine(String line, LocationType locationType) {
        List<String> tokens = tokenizeLine(line);
        
        return LocationBuilder.builder()
            .tokens(tokens)
            .locationType(locationType)
            .currentTime(LocalDateTime.now())
            .userId(getCurrentUserId())
            .build()
            .buildLocation()
            .map(this::saveLocation)
            .orElseThrow(() -> new LocationProcessingException("Failed to process location line"));
    }

    private List<String> tokenizeLine(String line) {
        return Arrays.stream(line.split("\\|"))
            .map(String::trim)
            .map(token -> token.isEmpty() ? "NULL" : token)
            .collect(Collectors.toList());
    }

    @Transactional
    private Location saveLocation(Location location) {
        try {
            return locationRepository.save(location);
        } catch (Exception e) {
            log.error("Failed to save location: {}", e.getMessage());
            throw new LocationPersistenceException("Failed to save location", e);
        }
    }
}

/**
 * Usage example with business rules validation
 */
@RestController
@RequestMapping("/api/locations")
public class LocationController {
    private final LocationService locationService;

    @PostMapping
    public ResponseEntity<Location> createLocation(
            @RequestBody String locationLine,
            @RequestParam LocationType locationType) {
        try {
            Location location = locationService.processLocationLine(locationLine, locationType);
            return ResponseEntity.ok(location);
        } catch (Exception e) {
            log.error("Failed to create location: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
