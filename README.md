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


## Prerequisites

- Java 17+
- Maven 3.8+
- Docker (for containerization optional if you use docker-compose file then use Docker)

## Running the Services

you can use Docker Compose to run all services together:

```bash
docker-compose up --build

