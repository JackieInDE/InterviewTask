## Project Startup Instructions

### MySQL Configuration

- Configure the MySQL **server address**, username, and password in the `application.yml` file.
- Before starting the service, execute the initialization DDL SQL scripts to create the required tables.
- Ensure that the MySQL service is running and accessible.

### Redis Configuration

- Set the Redis server address and port in the `application.yml` file.
- Verify that the Redis service is up and reachable.

### Application Startup and Endpoint Verification

- Start the application by running `src/main/java/com/meet5/SocialNetworkApplication.java`.
- After startup, verify the functionality by accessing:
  `http://{startUrl}:{startPort}/user/visit`

### Unit and Integration Testing

- Execute the test cases located in the `test` package to run unit and integration tests.

## Table Design Specification

### users Table

The users table stores the core information of platform users.  
Based on research across multiple social media platforms, it is standard practice to require users to provide their date of birth during registration.  
Since age is a dynamic attribute, it is calculated dynamically based on the birth date when needed, rather than being statically stored in the database, to maintain data accuracy and consistency.
```sql
CREATE TABLE users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  job VARCHAR(50),
  gender TINYINT NOT NULL DEFAULT 0 COMMENT '0-Female, 1-Male',
  birthday DATE NOT NULL,
  location_id INT NOT NULL,
  account_status VARCHAR(20) DEFAULT 'ACTIVE',
  relationship_status ENUM('Single', 'Married', 'AlreadyTaken', 'NotSpecified') DEFAULT 'NotSpecified',
  profile_picture_id BIGINT,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  created_by VARCHAR(100) NOT NULL DEFAULT 'sys',
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  updated_by VARCHAR(100) NOT NULL DEFAULT 'sys',
  KEY `idx_name` (name),
  KEY `idx_created_time` (created_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### likes Table and likes_log Table

The `likes` table records a user's like and unlike actions towards another user, along with timestamps.  
The `likes_log` table archives the history of like status changes for auditing purposes.  
As data volume grows, old records can be purged periodically through time-based partitioning.
```sql
CREATE TABLE likes (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  liker_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  liked_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0-liked, 1-canceled',
  updated_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY `idx_likes_liker_id` (liker_id),
  KEY `idx_likes_target_id` (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE likes_log (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  liker_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  status TINYINT NOT NULL DEFAULT 0 COMMENT '0-liked, 1-canceled',
  KEY `idx_likes_log_liker_id` (liker_id),
  KEY `idx_likes_log_target_id` (target_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```


### visits Table

This table stores user visit behavior — when one user visits another user's profile.  
Similar to the likes table, old visit records can be periodically purged through time-based partitioning as data grows.
```sql
CREATE TABLE visits (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  visitor_id BIGINT NOT NULL,
  target_id BIGINT NOT NULL,
  visited_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  KEY `idx_visits_visitor_id` (visitor_id),
  KEY `idx_visits_target_id_visited_time` (target_id, visited_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```



## API Design

### 1. Record User Visit - `/user/visit`

This API records a user's visit behavior and sends the event to the `RiskManagementService`.  
`RiskManagementService` uses Redis Lua scripts to monitor the number of visits and likes by a user within a 10-minute window.  
If the count exceeds 100, a positive detection is returned, prompting the `UserService` to update the user's account status from `ACTIVE` to `FRAUD`.

### 2. User Like/Unlike - `/user/like`

This API records a user's like action.

If the same request is submitted again, it toggles to unlike.  
The anti-fraud logic is consistent with `/user/visit`, where each action triggers fraud risk assessment and user status update if necessary.  
### 3. View Visitor Records - `/user/{id}/getVisitors`

This API retrieves the list of users who have visited the profile.  
Design considerations are as follows:  
- **Priority:** Recent visitors are listed first (sorted by visit time descending).
- **Secondary Consideration:** (Not yet implemented) Higher frequency visitors would be prioritized further.

Only visitors within the last month are retrieved, limited to the 10 most recent entries.

If a visitor accesses the profile multiple times in a single day, only the **latest visit** of that day is displayed to optimize user experience.

Authentication must be enforced to ensure that users can only view their own visitor records.

### 4. Bulk Insert Strategy

Large-scale inserts are handled via **concurrent execution combined with transaction isolation**.

- Each batch runs in an independent transaction to prevent oversized transactions and rollback risks.
- Concurrency control is applied to maintain database stability and achieve high throughput.


## To-Do List

### 1. Logging and Trace ID Integration

- Add structured logs at critical code points.
- Implement Trace ID propagation (e.g., using MDC) to support distributed tracing.

### 2. API Authentication and Authorization

- Enforce authentication for all APIs.
- Validate user sessions or tokens to prevent unauthorized access.  

### 3. Security Hardening

- Integrate Spring Security for centralized authentication and authorization.
- Protect sensitive endpoints with JWT authentication.
- Clearly identify publicly exposed APIs and implement strict access controls.

### 4. Error Code Definition

- Define a standardized error code framework.
- Error codes should include module IDs, error types, and unique identifiers.
- Provide detailed error messages for better troubleshooting and user communication.


## Microservice Decomposition Plan

### Microservices Overview

1. **api-gateway**
    - Acts as the single entry point for all client requests, handling traffic throttling, API version management, and request routing.

2. **auth-service**
    - Provides OAuth 2 and JWT-based authentication and authorization.

3. **user-service**
    - Manages user profiles and account status.

4. **interaction-service**
    - Processes user "like" and "visit" actions, aggregates statistics, and publishes domain events to the event bus.

5. **risk-service**
    - Consumes behavioral events asynchronously, performs fraud detection, and updates users' risk flags in the `user-service` via gRPC.

6. **notification-service**
    - Listens to behavioral events and sends real-time notifications to users who have been liked or visited.



### Communication Mechanisms

- **External APIs:** HTTP/REST interfaces with JSON payloads.
- **Internal Services:**
    - Synchronous communication via gRPC (Protocol Buffers).
    - Asynchronous integration via Kafka event streams.



### Data Consistency Strategy

- Prefer **transactional messaging** or **Saga patterns** for eventual consistency.
- Use **TCC** or **Two-Phase Commit (XA)** for strict consistency requirements.


### API Versioning Strategy

- URI-based versioning (e.g., `/v1/...`) and Spring HATEOAS for hypermedia-driven updates.
- Parallel deployments for at least 30 days during version transitions.
- Add `Deprecation` and `Sunset` headers for deprecated APIs.
- Gradual traffic shifting via API Gateway (canary releases based on version headers or paths).


### Resilience and Fault Tolerance Strategy

1. **Fault Isolation:**
   - Use Resilience4j (circuit breaking, bulkheading, retries) and Alibaba Sentinel (rate limiting).
2. **Observability and Alerts:**
   - Expose Prometheus metrics; visualize via Grafana and route alerts via Alertmanager.
3. **Distributed Tracing:**
   - Use OpenTelemetry SDK; store traces in Tempo or Jaeger.
4. **Rate Limiting and Auto-Scaling:**
   - Protect hot keys; scale pods automatically with HPA/KEDA based on resource usage or custom metrics.
5. **Deployment and Health Probes:**
   - Implement canary/blue-green deployments with Argo Rollouts; configure proper readiness and liveness probes.