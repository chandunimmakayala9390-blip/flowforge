package com.flowforge.core;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LoadBalancerTest {

    @Test
    void roundRobinDistributesConcurrentSelectionsAcrossBackends() throws Exception {
        BackendServer first = new BackendServer("first", "http://localhost:1");
        BackendServer second = new BackendServer("second", "http://localhost:2");
        BackendServer third = new BackendServer("third", "http://localhost:3");
        LoadBalancer balancer = new LoadBalancer(
                List.of(first, second, third), LoadBalancingStrategy.ROUND_ROBIN);

        var counts = new ConcurrentHashMap<String, AtomicInteger>();
        int workers = 12;
        int requestsPerWorker = 1000;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch start = new CountDownLatch(1);
        var futures = new java.util.ArrayList<java.util.concurrent.Future<?>>();

        for (int worker = 0; worker < workers; worker++) {
            futures.add(pool.submit(() -> {
                start.await();
                for (int i = 0; i < requestsPerWorker; i++) {
                    String id = balancer.selectServer().getId();
                    counts.computeIfAbsent(id, ignored -> new AtomicInteger())
                            .incrementAndGet();
                }
                return null;
            }));
        }

        start.countDown();
        try {
            for (var future : futures) {
                future.get(10, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        int expectedEach = workers * requestsPerWorker / 3;
        assertEquals(expectedEach, counts.get("first").get());
        assertEquals(expectedEach, counts.get("second").get());
        assertEquals(expectedEach, counts.get("third").get());
    }

    @Test
    void skipsUnhealthyBackends() {
        BackendServer healthy = new BackendServer("healthy", "http://localhost:1");
        BackendServer down = new BackendServer("down", "http://localhost:2");
        down.setHealthy(false);
        LoadBalancer balancer = new LoadBalancer(
                List.of(down, healthy), LoadBalancingStrategy.ROUND_ROBIN);

        assertSame(healthy, balancer.selectServer());
    }

    @Test
    void failsWhenNoBackendIsHealthy() {
        BackendServer down = new BackendServer("down", "http://localhost:2");
        down.setHealthy(false);
        LoadBalancer balancer = new LoadBalancer(
                List.of(down), LoadBalancingStrategy.ROUND_ROBIN);

        assertThrows(IllegalStateException.class, balancer::selectServer);
    }

    @Test
    void weightedRoundRobinRespectsWeights() {
        BackendServer light = new BackendServer("light", "http://localhost:1", 1);
        BackendServer heavy = new BackendServer("heavy", "http://localhost:2", 3);
        LoadBalancer balancer = new LoadBalancer(
                List.of(light, heavy), LoadBalancingStrategy.WEIGHTED_ROUND_ROBIN);

        int lightCount = 0;
        int heavyCount = 0;
        for (int i = 0; i < 40; i++) {
            if (balancer.selectServer() == light) {
                lightCount++;
            } else {
                heavyCount++;
            }
        }

        assertEquals(10, lightCount);
        assertEquals(30, heavyCount);
    }

    @Test
    void leastConnectionsChoosesTheLeastBusyBackend() {
        BackendServer busy = new BackendServer("busy", "http://localhost:1");
        BackendServer idle = new BackendServer("idle", "http://localhost:2");
        busy.incrementConnections();
        busy.incrementConnections();

        LoadBalancer balancer = new LoadBalancer(
                List.of(busy, idle), LoadBalancingStrategy.LEAST_CONNECTIONS);

        assertSame(idle, balancer.selectServer());
    }
}
