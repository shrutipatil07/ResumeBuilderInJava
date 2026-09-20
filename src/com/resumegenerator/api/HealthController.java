package com.resumegenerator.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

// ---------------------------------------------------------------
// HealthController — a simple REST endpoint to verify that the
// Spring Boot application is running and serving HTTP responses.
//
// @RestController combines:
//   @Controller  — marks this class as a Spring MVC controller
//   @ResponseBody — every method's return value is serialized
//                    directly to the HTTP response body (as JSON)
//                    instead of resolving a view/template name
//
// @RequestMapping("/api") — all endpoints in this controller
//   are prefixed with /api. So @GetMapping("/health") becomes
//   GET /api/health.
//
// WHY "/api" PREFIX?
//   When the React frontend is added later, it will serve static
//   files at "/" and call the backend at "/api/*". The prefix
//   cleanly separates frontend routes from backend endpoints.
// ---------------------------------------------------------------
@RestController
@RequestMapping("/api")
public class HealthController {

    // ---------------------------------------------------------------
    // GET /api/health
    //
    // Returns a simple JSON object confirming the API is running:
    //   {
    //     "status": "UP",
    //     "application": "Resume Builder API"
    //   }
    //
    // Spring Boot + Jackson automatically converts the Map to JSON.
    //
    // USE CASES:
    //   • Quick verification after starting the server
    //   • Health check for monitoring tools
    //   • First endpoint to test from the React frontend
    // ---------------------------------------------------------------
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "UP");
        response.put("application", "Resume Builder API");
        return response;
    }
}
