# FlowForge — Configurable HTTP Load Balancer

FlowForge is a Java-based HTTP load balancer built with Spring Boot. It forwards incoming HTTP requests to configured backend servers, supports multiple load-balancing strategies, and monitors backend health to help handle backend failures.

## Features

- **Round Robin:** Distributes requests across available backend servers in rotation.
- **Weighted Round Robin:** Distributes requests according to configured backend weights.
- **Least Connections:** Selects a backend based on its active connection count.
- **Health Monitoring:** Periodically checks backend health using Spring Boot Actuator.
- **Automatic Failover:** Skips unhealthy backends when selecting a server.
- **HTTP Reverse Proxy:** Forwards requests to the selected backend.
- **Query Parameter Forwarding:** Preserves query parameters when forwarding requests.
- **Error Handling:** Returns HTTP 503 Service Unavailable when no healthy backend is available.
- **Thread-Safe Connection Tracking:** Uses atomic counters to track active backend connections.

## Tech Stack

- Java 25
- Spring Boot 4
- Spring MVC
- Maven
- Spring Boot Actuator
- JUnit 5

## Prerequisites

- JDK 25
- Git
- A terminal or IDE such as Visual Studio Code

The project includes the Maven Wrapper, so a separate Maven installation is not required.

## Getting Started

### 1. Clone the repository

```bash
git clone https://github.com/YOUR-USERNAME/flowforge.git
cd flowforge
```

Replace `YOUR-USERNAME` with your GitHub username.

### 2. Build and test

On Windows:

```cmd
.\mvnw.cmd clean test
```

To package the application as an executable JAR:

```cmd
.\mvnw.cmd package
```

The generated JAR is located at:

```text
target/flowforge-0.0.1-SNAPSHOT.jar
```

### 3. Start the load balancer

```cmd
java -jar target/flowforge-0.0.1-SNAPSHOT.jar
```

By default, the main application listens on port `8080`.

### 4. Start backend servers

Run these commands in separate terminals, using the same JAR.

Backend 1:

```cmd
java -jar target/flowforge-0.0.1-SNAPSHOT.jar --server.port=8081 --flowforge.backend.name=backend-1
```

Backend 2:

```cmd
java -jar target/flowforge-0.0.1-SNAPSHOT.jar --server.port=8082 --flowforge.backend.name=backend-2
```

Ensure that the backend names and ports match the configuration in `application.properties`.

## Configuration

Backend servers, their URLs, weights, and the selected load-balancing strategy are configured in:

```text
src/main/resources/application.properties
```

The available strategies are:

- `ROUND_ROBIN`
- `WEIGHTED_ROUND_ROBIN`
- `LEAST_CONNECTIONS`

Change the configured strategy and backend weights to experiment with request distribution.

## Testing the Application

### Health endpoint

Open:

```text
http://localhost:8080/actuator/health
```

A healthy application should report an `UP` status.

### Proxy endpoint

Open:

```text
http://localhost:8080/proxy/backend-test?user=chandu
```

The response includes information about the backend that handled the request.

### Failover

Stop a backend process and send requests through the load balancer. The health checker can mark the stopped backend unhealthy, allowing requests to be routed to another healthy backend.

If no healthy backend is available, the application returns HTTP 503 Service Unavailable.

## Running Tests

Run:

```cmd
.\mvnw.cmd clean test
```

This runs the automated tests included in the project.

## Project Structure

```text
flowforge/
├── .mvn/
│   └── wrapper/                  # Maven Wrapper configuration
├── src/
│   ├── main/
│   │   ├── java/com/flowforge/
│   │   │   ├── FlowforgeApplication.java
│   │   │   └── core/
│   │   │       ├── BackendProperties.java
│   │   │       ├── BackendServer.java
│   │   │       ├── BackendUnavailableException.java
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── HealthCheckService.java
│   │   │       ├── LoadBalancer.java
│   │   │       ├── LoadBalancerConfig.java
│   │   │       ├── LoadBalancingStrategy.java
│   │   │       ├── ProxyController.java
│   │   │       └── ProxyService.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/
│       └── java/com/flowforge/
│           └── FlowforgeApplicationTests.java
├── .gitignore
├── HELP.md
├── mvnw
├── mvnw.cmd
├── pom.xml
└── README.md
```

### Key Components

- **`LoadBalancer.java`** — Selects a backend according to the configured load-balancing strategy.
- **`LoadBalancingStrategy.java`** — Defines the supported load-balancing strategies.
- **`BackendServer.java`** — Represents a backend server and tracks its health and active connections.
- **`HealthCheckService.java`** — Periodically checks backend health.
- **`ProxyController.java`** — Exposes the HTTP proxy endpoint.
- **`ProxyService.java`** — Forwards incoming requests to the selected backend.
- **`BackendProperties.java`** — Binds backend configuration from application properties.
- **`LoadBalancerConfig.java`** — Configures backend server instances.
- **`GlobalExceptionHandler.java`** — Handles errors such as when no healthy backend is available.
- **`application.properties`** — Configures backend URLs, weights, and load-balancing behavior.

## Future Improvements

- Add automated unit and integration tests for all load-balancing strategies.
- Add concurrent-load and stress testing.
- Improve HTTP header and response-body forwarding.
- Add metrics and monitoring dashboards.
- Add Docker support and deployment automation.

## License

This project is available for educational and portfolio purposes. Add a license if you intend to distribute it under specific reuse terms.
