package com.flowforge.core;

import java.util.concurrent.atomic.AtomicInteger;

public class BackendServer {

    private final String id;
    private final String baseUrl;
    private final int weight;

    private volatile boolean healthy;
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public BackendServer(String id, String baseUrl) {
        this(id, baseUrl, 1);
    }

    public BackendServer(String id, String baseUrl, int weight) {
        if (weight < 1) {
            throw new IllegalArgumentException(
                    "Backend weight must be at least 1");
        }

        this.id = id;
        this.baseUrl = baseUrl;
        this.weight = weight;
        this.healthy = true;
    }

    public String getId() {
        return id;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public int getWeight() {
        return weight;
    }

    public boolean isHealthy() {
        return healthy;
    }

    public void setHealthy(boolean healthy) {
        this.healthy = healthy;
    }

    public int getActiveConnections() {
        return activeConnections.get();
    }

    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementConnections() {
        activeConnections.updateAndGet(count -> Math.max(0, count - 1));
    }

    @Override
    public String toString() {
        return "BackendServer{" +
                "id='" + id + '\'' +
                ", baseUrl='" + baseUrl + '\'' +
                ", weight=" + weight +
                ", healthy=" + healthy +
                ", activeConnections=" + activeConnections.get() +
                '}';
    }
}