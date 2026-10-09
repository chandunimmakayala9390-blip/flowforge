package com.flowforge.core;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProxyController {

    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @RequestMapping(value = "/proxy/**", method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.DELETE,
            RequestMethod.PATCH
    })
    public ResponseEntity<String> proxy(
            HttpServletRequest request,
            @RequestBody(required = false) String body) {

        String requestUri = request.getRequestURI();
        String path = requestUri.substring("/proxy".length());

        if (path.isEmpty()) {
            path = "/";
        }

        String queryString = request.getQueryString();

        if (queryString != null && !queryString.isBlank()) {
            path += "?" + queryString;
        }

        HttpMethod method = HttpMethod.valueOf(request.getMethod());

        return proxyService.forward(path, method, body);
    }
}