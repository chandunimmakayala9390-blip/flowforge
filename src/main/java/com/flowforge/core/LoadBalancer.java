package com.flowforge.core;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LoadBalancer {

    private final List<BackendServer> servers;
    private final LoadBalancingStrategy strategy;
    private final AtomicInteger currentIndex = new AtomicInteger(0);
    private final AtomicInteger weightedIndex = new AtomicInteger(0);

    public LoadBalancer(
            List<BackendServer> servers,
            LoadBalancingStrategy strategy) {
        this.servers = servers;
        this.strategy = strategy;
    }

    public synchronized BackendServer selectServer() {
        List<BackendServer> healthyServers = servers.stream()
                .filter(BackendServer::isHealthy)
                .toList();

        if (healthyServers.isEmpty()) {
            throw new IllegalStateException(
                    "No healthy backend servers available");
        }

        return switch (strategy) {
            case ROUND_ROBIN -> selectRoundRobin(healthyServers);
            case LEAST_CONNECTIONS -> selectLeastConnections(healthyServers);
            case WEIGHTED_ROUND_ROBIN ->
                selectWeightedRoundRobin(healthyServers);
        };
    }

    private BackendServer selectRoundRobin(
            List<BackendServer> healthyServers) {
        int index = Math.floorMod(
                currentIndex.getAndIncrement(),
                healthyServers.size());

        return healthyServers.get(index);
    }

    private BackendServer selectLeastConnections(
            List<BackendServer> healthyServers) {
        return healthyServers.stream()
                .min((a, b) -> Integer.compare(
                        a.getActiveConnections(),
                        b.getActiveConnections()))
                .orElseThrow();
    }

    private BackendServer selectWeightedRoundRobin(
            List<BackendServer> healthyServers) {

        int totalWeight = healthyServers.stream()
                .mapToInt(BackendServer::getWeight)
                .sum();

        int position = Math.floorMod(
                weightedIndex.getAndIncrement(),
                totalWeight);

        for (BackendServer server : healthyServers) {
            position -= server.getWeight();

            if (position < 0) {
                return server;
            }
        }

        throw new IllegalStateException(
                "Unable to select a weighted backend");
    }
}