package com.flowforge.core;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;

@Configuration
public class LoadBalancerConfig {

        @Bean
        public List<BackendServer> backendServers(
                        BackendProperties properties) {

                return properties.getBackends().stream()
                                .map(config -> new BackendServer(
                                                config.getId(),
                                                config.getUrl(),
                                                config.getWeight()))
                                .toList();
        }

        @Bean
        public LoadBalancer loadBalancer(
                        List<BackendServer> backendServers,
                        Environment environment) {

                String strategyName = environment.getProperty(
                                "flowforge.load-balancer.strategy",
                                "ROUND_ROBIN");

                LoadBalancingStrategy strategy = LoadBalancingStrategy.valueOf(
                                strategyName.trim().toUpperCase());

                return new LoadBalancer(backendServers, strategy);
        }
}