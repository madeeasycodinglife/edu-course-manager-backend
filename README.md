# Microservices API Documentation IIT-Bombay Assignment

This repository contains various microservices with their respective APIs for authentication, user management, course management, and instance management. The services are designed with Spring Boot and secured using JWT tokens.

## Services Overview

### 1. Auth Service
Handles user authentication, including sign-up, sign-in, token refresh, and logout.

- **Sign-Up**: `POST /auth-service/sign-up`
    - Request Body:
      ```json
      {
        "fullName": "Pabitra Bera",
        "email": "pabitra@gmail.com",
        "password": "Apabitra@123",
        "phone": "8101187317",
        "roles": ["ADMIN"]
      }
      ```
- **Refresh Token**: `POST /auth-service/refresh-token/{token}`
- **Sign-In**: `POST /auth-service/sign-in`
    - Request Body:
      ```json
      {
        "email": "pabitra@gmail.com",
        "password": "Ap@123"
      }
      ```
- **Log-Out**: `POST /auth-service/log-out`
    - Request Body:
      ```json
      {
        "email": "pabitra@gmail.com",
        "accessToken": "{your_access_token}"
      }
      ```
- **Validate Token**: `POST /auth-service/validate-access-token/{token}`

### 2. User Service
Manages user details and profile updates.

- **Get User Details**: `GET /user-service/{email}`
    - Requires Bearer Token in the Authorization header.
- **Partial Update User**: `PATCH /user-service/partial-update/{email}`
    - Request Body:
      ```json
      {
        "fullName": "Pabitra Bera",
        "email": "pabitra@gmail.com",
        "password": "Apabitra@123",
        "phone": "8101187317",
        "roles": ["ADMIN"]
      }
      ```

### 3. Course Service
Handles course creation and management.

- **Create Course**: `POST /api/courses`
    - Request Body:
      ```json
      {
        "title": "Introduction to Computer Architecture",
        "courseCode": "CS 102",
        "description": "This course provides a basic introduction to the architecture and algorithms of computer systems."
      }
      ```
- **Delete Course**: `DELETE /api/courses/{courseId}`

### 4. Instance Service
Manages course instances based on year and semester.

- **Create Instance**: `POST /api/instances`
    - Request Body:
      ```json
      {
        "year": 2023,
        "semester": 1,
        "courseId": 1
      }
      ```
- **Get Instance by Year and Semester**: `GET /api/instances/{year}/{semester}`
- **Get Instance by Year, Semester, and Course ID**: `GET /api/instances/{year}/{semester}/{courseId}`
- **Delete Instance by Year, Semester, and Course ID**: `DELETE /api/instances/{year}/{semester}/{courseId}`

### 5. Config Server
Handles configuration updates and propagates changes across services.

- **Bus Refresh**: `POST /actuator/busrefresh`
    - Used to refresh dynamic configurations across microservices.

## Authentication and Authorization

**Important:** Many endpoints in these microservices require authentication and authorization. Access to these endpoints is role-specific, and attempting to access a restricted endpoint without the proper credentials or roles will result in an access denial.

- Ensure you include a valid JWT token in the Authorization header for endpoints that require it.
- Roles are dynamically managed and can be updated by modifying the configuration file in the GitHub repository. After updating roles, you can propagate the changes across all services by hitting the `POST /actuator/busrefresh` endpoint on the Config Server.


To include the information about accessing the H2 databases in the `README.md` file, you can add a dedicated section explaining how to access each service's H2 database through the `h2-console`, along with the default credentials and datasource URLs. Here's how you can update your `README.md` file:

---

## Accessing H2 Databases

Each microservice in this project is configured with an in-memory H2 database that can be accessed via a web interface. The H2 database can be accessed through the `/h2-console` endpoint of each service.

### Default Credentials

- **Username**: `sa`
- **Password**: `sa`

### H2 Database URLs

Below are the datasource URLs for each service:

1. **Auth Service**
    - **Datasource URL**: `jdbc:h2:mem:auth-service`
    - **Access Console**: [http://localhost:<auth-service-port>/h2-console](http://localhost:<auth-service-port>/h2-console)

2. **Instance Service**
    - **Datasource URL**: `jdbc:h2:mem:instance-service`
    - **Access Console**: [http://localhost:<instance-service-port>/h2-console](http://localhost:<instance-service-port>/h2-console)

3. **Course Service**
    - **Datasource URL**: `jdbc:h2:mem:course-service`
    - **Access Console**: [http://localhost:<course-service-port>/h2-console](http://localhost:<course-service-port>/h2-console)

4. **User Service**
    - **Datasource URL**: `jdbc:h2:mem:user-service`
    - **Access Console**: [http://localhost:<user-service-port>/h2-console](http://localhost:<user-service-port>/h2-console)

### How to Access the H2 Console

1. Start the microservices using Docker or your preferred method.
2. Open a web browser.
3. Navigate to the `/h2-console` endpoint of the desired service, for example, `http://localhost:8083/h2-console` for the `auth-service`.
4. Enter the credentials (`sa`/`sa`).
5. Use the corresponding datasource URL from the list above to connect to the database.

### Note

- Replace `<auth-service-port>`, `<instance-service-port>`, `<course-service-port>`, and `<user-service-port>` with the actual ports on which the respective services are running.
- Ensure that the service is up and running before accessing the H2 console.

---
You're absolutely correct! Redis in your setup is used for caching (leveraged through annotations like `@Cacheable` and `@CacheEvict`), while Kafka is employed for propagating configuration change events using Spring Cloud Bus. I apologize for the confusion earlier. Here’s the revised explanation:

---

## Prerequisites

- **Java 17+**: The application is built using Java 17, so you need at least this version installed.

- **Maven 3.8+**: Maven is required to manage project dependencies and build the application.

- **Docker**:
    - **For containerization (optional)**: If you plan to run your application and related services (like Zookeeper, Kafka, Redis, Zipkin) in containers.
    - **Docker Compose (optional)**: If you use a `docker-compose.yml` file to manage and orchestrate multiple services, Docker Compose is needed.

- **Zookeeper**:
    - Required for managing and coordinating Kafka clusters. Zookeeper ensures that the Kafka nodes are in sync.

- **Kafka**:
    - Used as a message broker with Spring Cloud Bus to propagate configuration changes across microservices in real-time.

- **Redis**:
    - Used as a caching layer in your Spring Boot application, typically integrated with Spring's caching abstraction using annotations like `@Cacheable` and `@CacheEvict`.

- **Zipkin**:
    - A distributed tracing system that helps gather timing data to troubleshoot latency problems in microservices architectures.

---

### Role of Each Component:

1. **Redis**:
    - Acts as a caching layer to store frequently accessed data, improving the performance of your application. Spring Boot's caching abstraction integrates Redis easily using annotations like `@Cacheable`, `@CacheEvict`, etc.

2. **Kafka** (with **Zookeeper**):
    - Kafka, orchestrated by Zookeeper, is used with Spring Cloud Bus to handle the broadcasting of configuration changes. This ensures that all microservices connected to the Config Server are updated with the latest configurations from your Git repository in real-time.

3. **Zipkin**:
    - Provides distributed tracing to monitor and troubleshoot latency issues across your microservices, giving you insights into the performance of your entire system.
    - http://localhost:9411/zipkin/       -> this the zipkin endpoint.
These tools work together to ensure your Spring Boot microservices architecture is efficient, scalable, and easy to manage.

---

> **Note:** I have not yet added a Docker-Compose file for this project as I am still in the process of learning how to run microservices in a Docker environment using Docker-Compose. However, if you are familiar with Docker, you can create a Docker-Compose file based on this project and run it successfully. The project has been thoroughly tested and works perfectly without any errors outside of a Docker environment. Once I complete my learning, I will update this repository with the Docker-Compose file.

---

## Running the Services

you can use Docker Compose to run all services together:

```bash
docker-compose up

