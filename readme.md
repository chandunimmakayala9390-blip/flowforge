# FlowForge — Concurrent HTTP Load Balancer

FlowForge is a Java 25 and Spring Boot HTTP reverse proxy that distributes incoming requests across configured backend instances. This iteration focuses on concurrent request routing, bounded network waits, health monitoring, and failover for retry-safe requests.

## Features

- **Concurrent backend selection:** Atomic counters avoid serializing every request through one synchronized method.
- **Three routing strategies:** Round Robin, Weighted Round Robin, and Least Connections.
- **Concurrent health checks:** A bounded health-check pool probes backends independently.
- **Health-aware routing:** Unhealthy backends are excluded from selection.
- **Bounded timeouts:** Proxy and health-check calls use configurable connect/read timeouts.
- **Conservative failover:** Retry-safe HTTP methods may try another backend after a transport failure. POST and PATCH are not automatically retried, avoiding possible duplicate side effects.
- **HTTP reverse proxy:** Handles GET, HEAD, POST, PUT, DELETE, PATCH, and OPTIONS; preserves query strings and forwards end-to-end request/response headers.
- **Graceful failure:** Returns HTTP 503 if no healthy backend is available or all allowed attempts fail.

Performance numbers are intentionally not claimed in this README until load tests have been run and recorded on a specific machine.

## Tech stack

- Java 25
- Spring Boot 4
- Spring MVC and RestClient
- Spring Boot Actuator
- Maven Wrapper
- JUnit 5

## Requirements

- JDK 25
- Git
- VS Code or another Java IDE

The repository includes the Maven Wrapper, so a separate Maven installation is not required.

## Run locally on Windows

Clone the repository:

    git clone https://github.com/chandunimmakayala9390-blip/flowforge.git
    cd flowforge

Run the tests and build the executable JAR:

    .\mvnw.cmd clean test
    .\mvnw.cmd package

Start the load balancer on port 8080:

    java -jar target\flowforge-0.0.1-SNAPSHOT.jar

Start each backend in a separate terminal:

    java -jar target\flowforge-0.0.1-SNAPSHOT.jar --server.port=8081 --flowforge.backend.name=backend-1

    java -jar target\flowforge-0.0.1-SNAPSHOT.jar --server.port=8082 --flowforge.backend.name=backend-2

Verify health and proxy requests:

    curl.exe http://localhost:8080/actuator/health
    curl.exe "http://localhost:8080/proxy/backend-test?user=chandu"

## Configuration

All settings are in src/main/resources/application.properties.

| Property | Default | Description |
|---|---:|---|
| flowforge.load-balancer.strategy | WEIGHTED_ROUND_ROBIN | ROUND_ROBIN, WEIGHTED_ROUND_ROBIN, or LEAST_CONNECTIONS |
| flowforge.proxy.connect-timeout-ms | 1000 | Backend connection timeout in milliseconds |
| flowforge.proxy.read-timeout-ms | 3000 | Backend response-read timeout in milliseconds |
| flowforge.proxy.max-attempts | 2 | Maximum attempts for retry-safe methods |
| flowforge.health-check.interval-ms | 5000 | Delay between health-check cycles |
| flowforge.health-check.timeout-ms | 800 | Health-probe connect/read timeout |

FlowForge marks a backend unhealthy when a proxied transport request fails. The scheduled health checker can mark it healthy again after its Actuator health endpoint responds successfully.

## Check distribution and failover

Send a series of requests:

    1..20 | ForEach-Object {
      curl.exe -s http://localhost:8080/proxy/backend-test
      Write-Output ""
    }

Then stop one backend, wait for a health-check cycle, and send requests again. Requests should continue through the remaining healthy backend. If no usable backend is available, the proxy responds with HTTP 503.

## Load testing

For a reproducible concurrency test, use k6 or Apache JMeter to send traffic to:

    http://localhost:8080/proxy/backend-test

Increase virtual users in stages—for example 10, 25, 50, then 100—and keep each run long enough to reach steady behavior (such as 60 seconds). Record:

- Throughput in requests per second
- p50, p95, and p99 response latency
- Error rate and HTTP status distribution
- CPU and memory of the proxy and backend processes

Keep the test machine, workload, duration, and backend count constant when comparing revisions. Only add measured throughput/latency figures to your resume after running the benchmark. Local results support comparisons on your machine; they do not guarantee production capacity.

## Tests

Run all tests:

    .\mvnw.cmd clean test

Load-balancer tests cover concurrent round-robin distribution, weighted distribution, least-connections selection, unhealthy-backend filtering, and the no-healthy-backend case.

## Project structure

    flowforge/
    ├── src/main/java/com/flowforge/
    │   ├── FlowforgeApplication.java
    │   └── core/
    │       ├── BackendProperties.java
    │       ├── BackendServer.java
    │       ├── BackendTestController.java
    │       ├── BackendUnavailableException.java
    │       ├── GlobalExceptionHandler.java
    │       ├── HealthCheckService.java
    │       ├── LoadBalancer.java
    │       ├── LoadBalancerConfig.java
    │       ├── LoadBalancingStrategy.java
    │       ├── ProxyController.java
    │       └── ProxyService.java
    ├── src/main/resources/application.properties
    ├── src/test/java/com/flowforge/
    └── pom.xml

## Resume summary

**FlowForge — Concurrent HTTP Load Balancer | Java, Spring Boot, Maven, JUnit**

- Built a configurable HTTP reverse proxy with round-robin, weighted round-robin, and least-connections routing across multiple backend instances.
- Removed global synchronization from backend selection and implemented concurrent health probes, configurable request timeouts, and bounded failover for retry-safe requests.
- Added concurrent distribution and routing-strategy tests; benchmark throughput and p95 latency under increasing concurrent loads before adding quantitative performance claims.

## License

This project is currently intended for educational and portfolio use. Add an explicit license if you want to grant others defined reuse permissions.
