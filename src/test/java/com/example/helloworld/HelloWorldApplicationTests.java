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



Go to the Canada Post Developer Program:

Visit https://www.canadapost-postescanada.ca/ac/support/api/
Click on "Get Started" or "Register Now"


Choose your service plan:

They offer different tiers based on usage:

Pay As You Go
Standard
Professional
Enterprise


Each plan has different pricing and transaction limits
You can start with Pay As You Go for testing


Create a business account:

Fill out the registration form
You'll need:

Business name
Business address
Contact information
Business registration number/GST number


Valid Canadian business registration is required


Complete verification:

Canada Post will verify your business information
May require additional documentation
This process can take 1-3 business days


Access the developer portal:

Once approved, you'll get access to the developer portal
Log in to https://www.canadapost-postescanada.ca/ac/
Navigate to "My Account" > "Web Service Credentials"
Your API key will be listed there


Test your API key:

You'll get a test environment first
Can make limited API calls to test integration
Once testing is successful, you can move to production



Important notes:

Pricing is based on number of lookups/transactions
You'll need a valid credit card for billing
Free trial may be available for initial testing
Support is available via phone and email during business hours
They provide API documentation and sample code once registered
