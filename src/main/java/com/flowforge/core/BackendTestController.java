package com.flowforge.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class BackendTestController {

    @Value("${server.port:8080}")
    private int port;

    @Value("${flowforge.backend.name:backend-default}")
    private String backendName;

    @GetMapping("/backend-test")
    public Map<String, String> backendTest(
            @RequestParam(required = false, defaultValue = "not-provided") String user) {

        return Map.of(
                "message", "Response from backend server",
                "backendName", backendName,
                "serverPort", String.valueOf(port),
                "user", user);
    }
}