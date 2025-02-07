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



import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.util.EntityUtils;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.HashMap;

public class CanadaPostAddressValidator {
    private static final Logger logger = LoggerFactory.getLogger(CanadaPostAddressValidator.class);
    private static final String API_BASE_URL = "https://ws1.postescanada-canadapost.ca/AddressComplete/Interactive/";
    private final String apiKey;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final int maxRetries;
    private final long retryDelayMs;

    public CanadaPostAddressValidator(String apiKey) {
        this(apiKey, 3, 1000); // Default: 3 retries with 1 second delay
    }

    public CanadaPostAddressValidator(String apiKey, int maxRetries, long retryDelayMs) {
        this.apiKey = apiKey;
        this.maxRetries = maxRetries;
        this.retryDelayMs = retryDelayMs;
        this.httpClient = HttpClients.custom()
            .setConnectionTimeoutMillis(5000)
            .setResponseTimeout(10, TimeUnit.SECONDS)
            .build();
        this.objectMapper = new ObjectMapper();
    }

    public AddressValidationResult validateAddress(CanadianAddress address) {
        try {
            // Step 1: Find matching addresses
            String searchText = buildSearchText(address);
            Map<String, String> findParams = new HashMap<>();
            findParams.put("Key", apiKey);
            findParams.put("SearchTerm", searchText);
            findParams.put("Country", "CAN");
            findParams.put("LanguagePreference", "EN");

            JsonNode findResult = executeRequest("Find/v2.10/json3.ws", findParams);
            
            if (!isSuccessful(findResult)) {
                return AddressValidationResult.error("Find request failed: " + getErrorMessage(findResult));
            }

            // Step 2: Retrieve full address details
            String moniker = findResult.path("Items").path(0).path("Id").asText();
            
            Map<String, String> retrieveParams = new HashMap<>();
            retrieveParams.put("Key", apiKey);
            retrieveParams.put("Id", moniker);

            JsonNode retrieveResult = executeRequest("Retrieve/v2.10/json3.ws", retrieveParams);

            if (!isSuccessful(retrieveResult)) {
                return AddressValidationResult.error("Retrieve request failed: " + getErrorMessage(retrieveResult));
            }

            return parseValidationResult(retrieveResult);

        } catch (Exception e) {
            logger.error("Error validating address", e);
            return AddressValidationResult.error("Validation failed: " + e.getMessage());
        }
    }

    private JsonNode executeRequest(String endpoint, Map<String, String> params) throws Exception {
        int attempts = 0;
        Exception lastException = null;

        while (attempts < maxRetries) {
            try {
                HttpPost request = new HttpPost(API_BASE_URL + endpoint);
                request.setEntity(new StringEntity(objectMapper.writeValueAsString(params)));
                request.setHeader("Content-Type", "application/json");

                String response = EntityUtils.toString(httpClient.execute(request).getEntity());
                return objectMapper.readTree(response);

            } catch (Exception e) {
                lastException = e;
                attempts++;
                
                if (attempts < maxRetries) {
                    Thread.sleep(retryDelayMs);
                }
            }
        }

        throw new RuntimeException("Failed after " + maxRetries + " attempts", lastException);
    }

    private String buildSearchText(CanadianAddress address) {
        StringBuilder sb = new StringBuilder();
        sb.append(address.getStreetNumber()).append(" ")
          .append(address.getStreetName()).append(", ")
          .append(address.getCity()).append(", ")
          .append(address.getProvince());
        
        if (address.getPostalCode() != null) {
            sb.append(" ").append(address.getPostalCode());
        }
        
        return sb.toString();
    }

    private boolean isSuccessful(JsonNode response) {
        return !response.path("Items").isEmpty();
    }

    private String getErrorMessage(JsonNode response) {
        return response.path("Error").asText("Unknown error");
    }

    private AddressValidationResult parseValidationResult(JsonNode retrieveResult) {
        JsonNode item = retrieveResult.path("Items").path(0);
        
        if (item.isMissingNode()) {
            return AddressValidationResult.error("No address found");
        }

        StandardizedAddress standardized = new StandardizedAddress();
        standardized.setStreetNumber(item.path("BuildingNumber").asText());
        standardized.setStreetName(item.path("Street").asText());
        standardized.setCity(item.path("City").asText());
        standardized.setProvince(item.path("ProvinceCode").asText());
        standardized.setPostalCode(item.path("PostalCode").asText());
        
        return AddressValidationResult.success(standardized);
    }

    // Inner classes for address representation
    public static class CanadianAddress {
        private String streetNumber;
        private String streetName;
        private String city;
        private String province;
        private String postalCode;

        // Getters and setters
        public String getStreetNumber() { return streetNumber; }
        public void setStreetNumber(String streetNumber) { this.streetNumber = streetNumber; }
        public String getStreetName() { return streetName; }
        public void setStreetName(String streetName) { this.streetName = streetName; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getPostalCode() { return postalCode; }
        public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    }

    public static class StandardizedAddress extends CanadianAddress {
        private boolean verified;
        
        public boolean isVerified() { return verified; }
        public void setVerified(boolean verified) { this.verified = verified; }
    }

    public static class AddressValidationResult {
        private final boolean success;
        private final StandardizedAddress standardizedAddress;
        private final String error;

        private AddressValidationResult(boolean success, StandardizedAddress address, String error) {
            this.success = success;
            this.standardizedAddress = address;
            this.error = error;
        }

        public static AddressValidationResult success(StandardizedAddress address) {
            return new AddressValidationResult(true, address, null);
        }

        public static AddressValidationResult error(String error) {
            return new AddressValidationResult(false, null, error);
        }

        public boolean isSuccess() { return success; }
        public StandardizedAddress getStandardizedAddress() { return standardizedAddress; }
        public String getError() { return error; }
    }

    // Example usage
    public static void main(String[] args) {
        CanadaPostAddressValidator validator = new CanadaPostAddressValidator("YOUR_API_KEY");
        
        CanadianAddress address = new CanadianAddress();
        address.setStreetNumber("123");
        address.setStreetName("Main Street");
        address.setCity("Toronto");
        address.setProvince("ON");
        address.setPostalCode("M5V 2T6");

        AddressValidationResult result = validator.validateAddress(address);
        
        if (result.isSuccess()) {
            StandardizedAddress standardized = result.getStandardizedAddress();
            System.out.println("Validated Postal Code: " + standardized.getPostalCode());
        } else {
            System.out.println("Error: " + result.getError());
        }
    }
}
