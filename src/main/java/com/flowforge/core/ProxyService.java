package com.flowforge.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class ProxyService {

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailer", "transfer-encoding", "upgrade", "host", "content-length");

    private final LoadBalancer loadBalancer;
    private final RestClient restClient;
    private final List<BackendServer> backends;
    private final int maxAttempts;

    public ProxyService(
            LoadBalancer loadBalancer,
            List<BackendServer> backends,
            @Value("${flowforge.proxy.connect-timeout-ms:1000}") long connectTimeoutMs,
            @Value("${flowforge.proxy.read-timeout-ms:3000}") long readTimeoutMs,
            @Value("${flowforge.proxy.max-attempts:2}") int maxAttempts) {
        this.loadBalancer = loadBalancer;
        this.backends = List.copyOf(backends);
        this.maxAttempts = Math.max(1, maxAttempts);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.max(100, connectTimeoutMs)));
        factory.setReadTimeout(Duration.ofMillis(Math.max(100, readTimeoutMs)));
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    public ResponseEntity<String> forward(String path, HttpMethod method, String body) {
        return forward(path, method, body, new HttpHeaders());
    }

    public ResponseEntity<String> forward(
            String path, HttpMethod method, String body, HttpHeaders inboundHeaders) {
        List<BackendServer> attempted = new ArrayList<>();

        // Do not automatically replay requests that may have non-idempotent side effects.
        int allowedAttempts = isSafeToRetry(method)
                ? Math.min(maxAttempts, backends.size())
                : 1;

        RestClientException lastFailure = null;
        for (int attempt = 0; attempt < allowedAttempts; attempt++) {
            BackendServer server;
            try {
                server = loadBalancer.selectServer();
            } catch (IllegalStateException exception) {
                throw new BackendUnavailableException(
                        "No healthy backend servers available", exception);
            }

            if (attempted.contains(server)) {
                BackendServer alternative = backends.stream()
                        .filter(BackendServer::isHealthy)
                        .filter(candidate -> !attempted.contains(candidate))
                        .findFirst()
                        .orElse(null);
                if (alternative == null) {
                    break;
                }
                server = alternative;
            }

            attempted.add(server);
            server.incrementConnections();
            try {
                return send(server.getBaseUrl() + path, method, body, inboundHeaders);
            } catch (RestClientException exception) {
                lastFailure = exception;
                server.setHealthy(false);
            } finally {
                server.decrementConnections();
            }
        }

        throw new BackendUnavailableException(
                "Backend request failed; attempted " + attempted.size() + " server(s)",
                lastFailure);
    }

    private ResponseEntity<String> send(
            String targetUrl, HttpMethod method, String body, HttpHeaders inboundHeaders) {
        RestClient.RequestBodySpec request = restClient.method(method).uri(targetUrl);
        request.headers(headers -> inboundHeaders.forEach((name, values) -> {
            if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                headers.put(name, new ArrayList<>(values));
            }
        }));

        if (body != null && method != HttpMethod.GET && method != HttpMethod.HEAD) {
            request.body(body);
        }

        return request.exchange((clientRequest, clientResponse) -> {
            HttpHeaders responseHeaders = new HttpHeaders();
            clientResponse.getHeaders().forEach((name, values) -> {
                if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase(Locale.ROOT))) {
                    responseHeaders.put(name, new ArrayList<>(values));
                }
            });

            String responseBody = null;
            try (var input = clientResponse.getBody()) {
                if (input != null && method != HttpMethod.HEAD) {
                    responseBody = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
            } catch (IOException exception) {
                throw new RestClientException("Unable to read backend response", exception);
            }

            return ResponseEntity.status(clientResponse.getStatusCode())
                    .headers(responseHeaders)
                    .body(responseBody);
        });
    }

    private boolean isSafeToRetry(HttpMethod method) {
        return method == HttpMethod.GET
                || method == HttpMethod.HEAD
                || method == HttpMethod.PUT
                || method == HttpMethod.DELETE
                || method == HttpMethod.OPTIONS;
    }
}
