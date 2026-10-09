package com.flowforge.core;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Service
public class ProxyService {

    private final LoadBalancer loadBalancer;
    private final RestClient restClient;

    public ProxyService(LoadBalancer loadBalancer) {
        this.loadBalancer = loadBalancer;
        this.restClient = RestClient.create();
    }

    public ResponseEntity<String> forward(
            String path,
            HttpMethod method,
            String body) {

        BackendServer server = loadBalancer.selectServer();
        server.incrementConnections();

        try {
            String targetUrl = server.getBaseUrl() + path;

            RestClient.RequestBodySpec request = restClient
                    .method(method)
                    .uri(targetUrl);

            if (body != null && !body.isBlank()
                    && method != HttpMethod.GET
                    && method != HttpMethod.HEAD) {
                request.body(body);
            }

            return request.exchange((req, response) -> {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(response.getHeaders());

                String responseBody = response.getBody() == null
                        ? null
                        : new String(
                                response.getBody().readAllBytes(),
                                java.nio.charset.StandardCharsets.UTF_8);

                return ResponseEntity
                        .status(response.getStatusCode())
                        .headers(headers)
                        .body(responseBody);
            });

        } catch (RestClientException exception) {
            throw new BackendUnavailableException(
                    "Request failed for backend " + server.getId(),
                    exception);
        } finally {
            server.decrementConnections();
        }
    }
}
