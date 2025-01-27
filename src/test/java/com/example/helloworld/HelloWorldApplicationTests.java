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

    LocBMChgCd (24) -> LOCBMCHGCD varchar(1)
LocHSPFTPrem (25) -> LOCAPREM numeric(10,2)  // HSP
LocHSPPremWrit (26) -> LOCPREMIUM numeric(10,2)
LocHSPComm (27) -> LOCCOMMISSION numeric(10,2)
LocHSPDeduct (28) -> LOCDEDUCT numeric(10,2)
LocSLCFTPrem (29) -> LOCAPREM numeric(10,2)  // SLC  
LocSLCPremWrit (30) -> LOCPREMIUM numeric(10,2)
LocSLCComm (31) -> LOCCOMMISSION numeric(10,2) 
LocSLCDeduct (32) -> LOCDEDUCT numeric(10,2)


}
