# SafetyPin Post Service

This repository contains the code for the SafetyPin Post Service. This service is designed to handle post-related operations, including creating, retrieving, and managing posts and categories. It also includes features for voting and file handling.

---

## Purpose

The primary purpose of this service is to provide a robust and efficient way to manage user-generated content within the SafetyPin application. It is built to be easily integrated with AI systems for analysis, code review, and other automated processes.

---

## Features

1. **Post Management:** Create, retrieve, update, and delete posts.
2. **Category Management:** Create, retrieve, and update categories for posts.
3. **Voting:** Handle user votes (upvotes and downvotes) on posts.
4. **File Handling:** Generate pre-signed URLs for secure file uploads to AWS S3.
5. **Location-based Filtering:** Retrieve posts based on distance.
6. **Security:** Implements security configurations.
7. **Exception Handling:** Comprehensive global exception handling for various scenarios.

---

## Code Organization

The codebase is organized as follows:

| Directory                                      | Description                                                                     |
| ---------------------------------------------- | ------------------------------------------------------------------------------- |
| `pom.xml`                                      | Maven configuration file.                                                       |
| `src/main/java/com/safetypin/post/config/`     | Configuration files for geometry and security.                                  |
| `src/main/java/com/safetypin/post/controller/` | REST controllers for handling category, post, S3, and vote related requests.    |
| `src/main/java/com/safetypin/post/dto/`        | Data Transfer Objects (DTOs) for requests and responses.                        |
| `src/main/java/com/safetypin/post/exception/`  | Custom exceptions used within the application.                                  |
| `src/main/java/com/safetypin/post/model/`      | JPA entities representing the data model (Post, Category, Comment, Vote, etc.). |
| `src/main/java/com/safetypin/post/repository/` | Spring Data JPA repositories for database interaction.                          |
| `src/main/java/com/safetypin/post/security/`   | Classes related to JWT security.                                                |
| `src/main/java/com/safetypin/post/seeder/`     | Class for seeding initial data (for development).                               |
| `src/main/java/com/safetypin/post/service/`    | Service layer containing business logic.                                        |
| `src/main/java/com/safetypin/post/utils/`      | Utility class for distance calculations.                                        |
| `src/test/java/com/safetypin/post/`            | Test classes for various components.                                            |

---

## Important Notes

- This document represents a subset of the codebase.
- Binary files might be excluded.
- Files matching `.gitignore` patterns are excluded.
- The files are sorted by Git change count.
- This file is read-only. Modify the original repository files.
- Handle this file with the same security as the original repository, as it might contain sensitive information.

---

## Usage Guidelines

When working with this codebase:

1. Treat the provided file as read-only.
2. Use the file paths to distinguish between different files.
3. Handle sensitive information with care.

---

## Getting Started

### Prerequisites

- Java Development Kit (JDK)
- Maven
- A database (e.g., PostgreSQL)
- AWS credentials (if you need to use S3 functionality)

### Installation

1. Clone the repository.
2. Configure the database connection in `application.properties`.
3. If S3 is used, configure AWS credentials.
4. Build the project using Maven: `mvn clean install`

### Running the Application

1. Run the `PostApplication.java` file.

### Testing

- To run the tests, use Maven: `mvn test`

---

## Key Classes and Functionalities

### Core Components

- **PostController:** Handles post-related REST API endpoints, including creating posts, retrieving posts (with filtering and pagination), and deleting posts.
- **PostService:** Contains the business logic for post operations, including validation, saving to the repository, and handling complex queries for post retrieval.
- **CategoryController:** Manages categories, providing endpoints for creating, retrieving, and updating category names.
- **CategoryService:** Handles category-related business logic, including creating, retrieving, and updating categories, with validation and interaction with the CategoryRepository.
- **S3Controller:** Provides an endpoint for generating pre-signed URLs, enabling secure file uploads to AWS S3.
- **S3Service:** Encapsulates the logic for generating pre-signed URLs using the AWS SDK, handling potential exceptions.
- **DistanceCalculator:** A utility class that provides a method for calculating geographical distances between two points using the Haversine formula.

---

## Running Tests

To run all unit and integration tests, execute:

```
mvn test
```

Tests are written using JUnit 5 and cover controllers, services, repository interactions, and utility components such as OTP generation and validation.

## Development

- **Code Style:** The project adheres to standard Java coding conventions and uses Lombok to reduce boilerplate.
- **Continuous Integration:** Integration with CI tools is recommended. Test coverage is ensured using Maven Surefire and Failsafe plugins.
- **Debugging:** Utilize Spring Boot DevTools for hot-reloading during development.

## Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository.
2. Create a new feature branch (git checkout -b feature/YourFeature).
3. Commit your changes (git commit -m 'Add some feature').
4. Push to the branch (git push origin feature/YourFeature).
5. Open a Pull Request.

Please ensure that your code adheres to the existing coding style and that all tests pass before submitting your PR.

## License

This project is licensed under the
Creative Commons Attribution-NoDerivatives 4.0 International (CC BY-ND 4.0)
License. See the LICENSE file for details.
