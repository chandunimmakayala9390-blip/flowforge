package com.flowforge.core;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class HealthCheckService {

    private final List<BackendServer> servers;
    private final RestClient restClient;
    private final ExecutorService healthCheckPool;

    public HealthCheckService(
            List<BackendServer> servers,
            @Value("${flowforge.health-check.timeout-ms:800}") long timeoutMs) {
        this.servers = List.copyOf(servers);

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(Math.max(100, timeoutMs)));
        factory.setReadTimeout(Duration.ofMillis(Math.max(100, timeoutMs)));
        this.restClient = RestClient.builder().requestFactory(factory).build();

        int poolSize = Math.max(1, Math.min(this.servers.size(), 8));
        this.healthCheckPool = Executors.newFixedThreadPool(poolSize, runnable -> {
            Thread thread = new Thread(runnable, "flowforge-health-check");
            thread.setDaemon(true);
            return thread;
        });
    }

    @Scheduled(fixedDelayString = "${flowforge.health-check.interval-ms:5000}")
    public void checkBackends() {
        // A slow backend does not make the checks for every other backend wait.
        for (BackendServer server : servers) {
            healthCheckPool.submit(() -> checkBackend(server));
        }
    }

    private void checkBackend(BackendServer server) {
        try {
            restClient.get()
                    .uri(server.getBaseUrl() + "/actuator/health")
                    .retrieve()
                    .toBodilessEntity();
            server.setHealthy(true);
        } catch (Exception exception) {
            server.setHealthy(false);
        }
    }
}
