package com.example.helloworld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HelloWorldApplication {

    public static void main(String[] args) {
        SpringApplication.run(HelloWorldApplication.class, args);
    }

}



       LocLineNo(0),                // First field - Location line number
        LocName(1),                  // Building name/number
        LocAddressType(2),          // Address type
        LocParcel(3),               // Parcel number
        LocLot(4),                  // Lot number
        LocBlock(5),                // Block number
        LocPlan(6),                 // Plan number
        LocQuarter(7),              // Quarter section
        LocSection(8),              // Section number
        LocTownship(9),             // Township
        LocRange(10),               // Range
        LocMeridian(11),            // Meridian
        LocCivSuiteNo(12),          // Suite number
        LocCivStreetNo(13),         // Street number
        LocCivStreetName(14),       // Street name
        LocStreetCode(15),          // Street code
        LocStreetDirection(16),     // Street direction
        LocLocationDesc(17),        // Location description
        LocCity(18),                // City
        LocProv(19),                // Province
        LocPostCode(20),            // Postal code
        LocNearInd(21),             // Near indicator
        LocNearLocName(22),         // Near location name
        LocWithinLocName(23),       // Within location name

        // Homeowner specific fields (24-32)
        LocBMChgCd(24),             // BM Change code
        LocHSPFTPrem(25),           // HSP FT Premium
        LocHSPPremWrit(26),         // HSP Premium Written
        LocHSPComm(27),             // HSP Commission
        LocHSPDeduct(28),           // HSP Deductible
        LocSLCFTPrem(29),           // SLC FT Premium
        LocSLCPremWrit(30),         // SLC Premium Written
        LocSLCComm(31),             // SLC Commission
        LocSLCDeduct(32),           // SLC Deductible

        // Commercial specific fields (24-43)
        LocationBusCode(24),         // Business code
        LocBMCov(25),               // BM Coverage
        CommLocBMChgCd(26),         // Commercial BM Change code
        LocBuildingLimit(27),        // Building limit
        LocDeduct(28),              // Deductible
        LocContentsLimit(29),       // Contents limit
        LocContentsDeduct(30),      // Contents deductible
        PolCONLimit(31),            // Policy CON limit
        LocBIForm1(32),             // Business interruption form 1
        LocBILimit1(33),            // Business interruption limit 1
        LocBIForm2(34),             // Business interruption form 2
        LocBILimit2(35),            // Business interruption limit 2
        LocBIForm3(36),             // Business interruption form 3
        LocBILimit3(37),            // Business interruption limit 3
        LocBIForm4(38),             // Business interruption form 4
        LocBILimit4(39),            // Business interruption limit 4
        LocBIForm5(40),             // Business interruption form 5
        LocBILimit5(41),            // Business interruption limit 5
        LocBIForm6(42),             // Business interruption form 6
        LocBILimit6(43);            // Business interruption limit 6
