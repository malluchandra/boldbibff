package com.example.boldbi.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@RestController
@RequestMapping("/boldbiinternal/")
public class BoldbiProxyController {

    private static final Logger logger = Logger.getLogger(BoldbiProxyController.class.getName());

    @Value("${boldbi.api.host}")
    private String boldbiHost;

    @Value("${boldbi.api.username}")
    private String username;

    @Value("${boldbi.api.embedSecret}")
    private String embedSecret;

    private final RestTemplate restTemplate;

    public BoldbiProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @GetMapping("/**")
    public ResponseEntity<?> proxyRequest(HttpServletRequest request) {
        try {
            // Print headers
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                System.out.println(headerName + ": " + request.getHeader(headerName));
            }

            // Get the token
            String token = fetchToken();

            // Prepare the target URL
            String targetPath = request.getRequestURI().replace("/boldbiinternal", "");
            String targetUrl = boldbiHost + targetPath;

            // Forward the request to the target URL
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(token);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(null, headers);

            // Log request details
            logger.info("Forwarding request to: " + targetUrl);
            logger.info("Request Headers: " + headers.toString());

            ResponseEntity<String> response = restTemplate.exchange(targetUrl, HttpMethod.GET, entity, String.class);

            // Log response details
            logger.info("Response Status Code: " + response.getStatusCode());
            logger.info("Response Body: " + response.getBody());

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());

        } catch (Exception e) {
            logger.severe("Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error: " + e.getMessage());
        }
    }

    private String fetchToken() {
        String tokenUrl = boldbiHost + "/bi/api/site/site1/token";

        Map<String, String> tokenRequestBody = new HashMap<>();
        tokenRequestBody.put("username", username);
        tokenRequestBody.put("embed_secret", embedSecret);
        tokenRequestBody.put("grant_type", "embed_secret");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(tokenRequestBody, headers);

        // Log token request details
        logger.info("Fetching token from: " + tokenUrl);
        logger.info("Token Request Body: " + tokenRequestBody.toString());

        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        // Log token response details
        logger.info("Token Response Status Code: " + response.getStatusCode());
        logger.info("Token Response Body: " + response.getBody());

        if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
            return response.getBody().get("access_token").toString();
        } else {
            throw new RuntimeException("Failed to fetch token");
        }
    }
}
