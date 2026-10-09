package com.flowforge.core;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class HealthCheckService {

    private final java.util.List<BackendServer> servers;
    private final RestClient restClient;

    public HealthCheckService(
            java.util.List<BackendServer> servers) {
        this.servers = servers;
        this.restClient = RestClient.create();
    }

    @Scheduled(fixedDelay = 5000)
    public void checkBackends() {
        for (BackendServer server : servers) {
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
}
