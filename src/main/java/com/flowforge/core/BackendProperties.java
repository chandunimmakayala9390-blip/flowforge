package com.flowforge.core;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "flowforge")
public class BackendProperties {

    private List<BackendConfig> backends = new ArrayList<>();

    public List<BackendConfig> getBackends() {
        return backends;
    }

    public void setBackends(List<BackendConfig> backends) {
        this.backends = backends;
    }

    public static class BackendConfig {

        private String id;
        private String url;
        private int weight = 1;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getWeight() {
            return weight;
        }

        public void setWeight(int weight) {
            this.weight = weight;
        }
    }
}
