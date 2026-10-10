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

## Tech stack

- Java 25
- Spring Boot 4
- Spring MVC and RestClient
- Spring Boot Actuator
- Maven Wrapper
- JUnit 5
- k6 for local load testing

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

All settings are in `src/main/resources/application.properties`.

| Property | Default | Description |
|---|---:|---|
| `flowforge.load-balancer.strategy` | `WEIGHTED_ROUND_ROBIN` | `ROUND_ROBIN`, `WEIGHTED_ROUND_ROBIN`, or `LEAST_CONNECTIONS` |
| `flowforge.proxy.connect-timeout-ms` | 1000 | Backend connection timeout in milliseconds |
| `flowforge.proxy.read-timeout-ms` | 3000 | Backend response-read timeout in milliseconds |
| `flowforge.proxy.max-attempts` | 2 | Maximum attempts for retry-safe methods |
| `flowforge.health-check.interval-ms` | 5000 | Delay between health-check cycles |
| `flowforge.health-check.timeout-ms` | 800 | Health-probe connect/read timeout |

FlowForge marks a backend unhealthy when a proxied transport request fails. The scheduled health checker can mark it healthy again after its Actuator health endpoint responds successfully.

## Check distribution and failover

Send a series of requests:

    1..20 | ForEach-Object {
      curl.exe -s http://localhost:8080/proxy/backend-test
      Write-Output ""
    }

Then stop one backend, wait for a health-check cycle, and send requests again. Requests should continue through the remaining healthy backend. If no usable backend is available, the proxy responds with HTTP 503.

## Load testing

The included k6 script is at `tests/load/flowforge.js`. Start the proxy and both backends, then run:

    k6 run tests/load/flowforge.js

The script ramps through 10, 25, 50, and 100 virtual users and checks the HTTP error rate and p95 latency. For shorter controlled runs, you can override the script's load profile:

    k6 run --vus 10 --duration 30s tests/load/flowforge.js

### Initial local measurements

Initial measurements were collected on **Windows 11**, using **Java 25.0.1**, **k6 2.2.0**, one FlowForge proxy, and two local backend instances on the same machine. These are local observations, not production-capacity claims.

| Virtual users | Duration | Requests/sec reported by k6 | p95 latency | Failed HTTP requests | Outcome |
|---:|---:|---:|---:|---:|---|
| 10 | 30 s | 5,981.13 | 2.53 ms | 0.00% | Both configured thresholds passed |
| 25 | 30 s | 2,623.93 | 9.76 ms | 12.18% | Failed error-rate threshold |
| 100 (ramped test) | 2 min | 2,175.71 | 5.79 ms | 15.23% | Failed error-rate threshold |

**Interpretation and limitations**

- The 10-VU run completed 179,441 requests with 0% reported HTTP request failures and passed the script's configured thresholds.
- The 25-VU and ramped 100-VU runs reported Windows socket errors: `Only one usage of each socket address (protocol/network address/port) is normally permitted`. Their failure rates mean these runs did not pass the reliability target.
- The socket errors suggest a local connection/resource issue may be involved, but the cause has **not** been confirmed. These measurements do not establish FlowForge's maximum capacity or prove that the proxy itself is the bottleneck.
- Treat the figures as preliminary until the higher-concurrency test environment is diagnosed and the benchmark is repeated. Results from localhost depend on the machine, workload, and test setup.

For meaningful comparisons between code changes, keep the machine, workload, duration, and backend count constant. Record throughput, p50/p95/p99 latency, HTTP error rate/status distribution, and proxy/backend CPU and memory. Do not present preliminary local measurements as production performance.

## Tests

Run all tests:

    .\mvnw.cmd clean test

The current test run completed successfully: **6 tests, 0 failures, 0 errors**. Load-balancer tests cover concurrent round-robin distribution, weighted distribution, least-connections selection, unhealthy-backend filtering, and the no-healthy-backend case.

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
    ├── src/test/java/com/flowforge/core/LoadBalancerTest.java
    ├── tests/load/flowforge.js
    └── pom.xml

## Resume summary

**FlowForge — Concurrent HTTP Load Balancer | Java, Spring Boot, Maven, JUnit, k6**

- Built a configurable HTTP reverse proxy with round-robin, weighted round-robin, and least-connections routing across multiple backend instances.
- Implemented concurrent backend selection, concurrent health probes, configurable request timeouts, and bounded failover for retry-safe requests; verified routing and failover behavior.
- Added unit tests (6/6 passing) and a local k6 benchmark. An initial 10-VU, 30-second run reported 5,981 requests/sec, 2.53 ms p95 latency, and 0% HTTP request failures; higher-concurrency runs remain under investigation.

## License

This project is currently intended for educational and portfolio use. Add an explicit license if you want to grant others defined reuse permissions.
