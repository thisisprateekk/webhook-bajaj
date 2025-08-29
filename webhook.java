// pom.xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>webhook-sql-app</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-logging</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>

// src/main/java/com/example/WebhookSqlApplication.java
package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class WebhookSqlApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebhookSqlApplication.class, args);
    }
}

// src/main/java/com/example/dto/WebhookRequest.java
package com.example.dto;

public class WebhookRequest {
    private String name;
    private String regNo;
    private String email;

    public WebhookRequest() {}

    public WebhookRequest(String name, String regNo, String email) {
        this.name = name;
        this.regNo = regNo;
        this.email = email;
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegNo() { return regNo; }
    public void setRegNo(String regNo) { this.regNo = regNo; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

// src/main/java/com/example/dto/WebhookResponse.java
package com.example.dto;

public class WebhookResponse {
    private String webhook;
    private String accessToken;

    public WebhookResponse() {}

    // Getters and Setters
    public String getWebhook() { return webhook; }
    public void setWebhook(String webhook) { this.webhook = webhook; }

    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
}

// src/main/java/com/example/dto/SqlSubmissionRequest.java
package com.example.dto;

public class SqlSubmissionRequest {
    private String finalQuery;

    public SqlSubmissionRequest() {}

    public SqlSubmissionRequest(String finalQuery) {
        this.finalQuery = finalQuery;
    }

    // Getters and Setters
    public String getFinalQuery() { return finalQuery; }
    public void setFinalQuery(String finalQuery) { this.finalQuery = finalQuery; }
}

// src/main/java/com/example/service/WebhookService.java
package com.example.service;

import com.example.dto.SqlSubmissionRequest;
import com.example.dto.WebhookRequest;
import com.example.dto.WebhookResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class WebhookService {
    
    private static final Logger logger = LoggerFactory.getLogger(WebhookService.class);
    
    private static final String GENERATE_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";
    private static final String SUBMIT_WEBHOOK_URL = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
    
    @Autowired
    private RestTemplate restTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void handleApplicationReady() {
        logger.info("Application started, initiating webhook process...");
        try {
            processWebhookFlow();
        } catch (Exception e) {
            logger.error("Error in webhook process: ", e);
        }
    }

    private void processWebhookFlow() {
        // Step 1: Generate webhook
        WebhookResponse webhookResponse = generateWebhook();
        
        if (webhookResponse != null && webhookResponse.getWebhook() != null) {
            logger.info("Webhook generated successfully: {}", webhookResponse.getWebhook());
            
            // Step 2: Prepare SQL solution
            String sqlQuery = getSqlSolution();
            
            // Step 3: Submit solution
            submitSolution(webhookResponse.getAccessToken(), sqlQuery);
        } else {
            logger.error("Failed to generate webhook");
        }
    }

    private WebhookResponse generateWebhook() {
        try {
            WebhookRequest request = new WebhookRequest("John Doe", "REG12347", "john@example.com");
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            
            HttpEntity<WebhookRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<WebhookResponse> response = restTemplate.exchange(
                GENERATE_WEBHOOK_URL,
                HttpMethod.POST,
                entity,
                WebhookResponse.class
            );
            
            logger.info("Webhook generation response: {}", response.getStatusCode());
            return response.getBody();
            
        } catch (Exception e) {
            logger.error("Error generating webhook: ", e);
            return null;
        }
    }

    private String getSqlSolution() {
        return "SELECT e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME, " +
               "COUNT(e2.EMP_ID) AS YOUNGER_EMPLOYEES_COUNT " +
               "FROM EMPLOYEE e1 " +
               "JOIN DEPARTMENT d ON e1.DEPARTMENT = d.DEPARTMENT_ID " +
               "LEFT JOIN EMPLOYEE e2 ON e1.DEPARTMENT = e2.DEPARTMENT AND e2.DOB > e1.DOB " +
               "GROUP BY e1.EMP_ID, e1.FIRST_NAME, e1.LAST_NAME, d.DEPARTMENT_NAME " +
               "ORDER BY e1.EMP_ID DESC";
    }

    private void submitSolution(String accessToken, String sqlQuery) {
        try {
            SqlSubmissionRequest request = new SqlSubmissionRequest(sqlQuery);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", accessToken);
            
            HttpEntity<SqlSubmissionRequest> entity = new HttpEntity<>(request, headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                SUBMIT_WEBHOOK_URL,
                HttpMethod.POST,
                entity,
                String.class
            );
            
            logger.info("Solution submitted successfully. Status: {}, Response: {}", 
                       response.getStatusCode(), response.getBody());
            
        } catch (Exception e) {
            logger.error("Error submitting solution: ", e);
        }
    }
}

// src/main/java/com/example/config/RestTemplateConfig.java
package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}

// src/main/resources/application.properties
# Application properties
spring.application.name=webhook-sql-app
server.port=8080
logging.level.com.example=INFO
logging.level.org.springframework.web.client=DEBUG