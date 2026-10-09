package com.flowforge;

import com.flowforge.core.BackendProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(BackendProperties.class)
@EnableScheduling
public class FlowforgeApplication {

	public static void main(String[] args) {
		SpringApplication.run(FlowforgeApplication.class, args);
	}
}
