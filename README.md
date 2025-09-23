# E-Commerce Order Processing System

A comprehensive Spring Boot 3.5.6 application implementing advanced order processing with transactional integrity, async event handling, and robust testing. Built with Java 21, this system demonstrates enterprise-level e-commerce backend capabilities with proper database management, security, and scalability features.

## 🎯 Task Implementation Overview

This project implements a complete **E-Commerce Order Processing System** with the following core requirements:

### ✅ **Completed Requirements**

**A. Pagination** ✅
- Implemented paginated API to fetch orders with page, size, and sorting by `created_at`
- Uses Spring Data JPA `Pageable` with custom `PaginatedResponse` wrapper
- Supports sorting by creation date (descending by default)

**B. Transactional Order Creation** ✅
- Created transactional method for order creation with multiple items
- Implements atomic stock updates using `@Transactional` and `SELECT FOR UPDATE`
- Prevents race conditions with proper locking mechanisms

**C. Async Event Processing within Transaction** ✅
- Implements `OrderCreatedEvent` with `@TransactionalEventListener`
- Event only published after successful transaction commit (`AFTER_COMMIT`)
- Async processing for email confirmation and logging (mocked)

**D. Exception Handling** ✅
- Global exception handler with `@RestControllerAdvice`
- Handles `InsufficientStockException` and `PaymentFailedException`
- Proper HTTP status codes and rollback mechanisms

**E. Native Query for Reporting** ✅
- Native SQL query for high-value orders (`total_amount > 1000`)
- Custom projection interface `OrderReportRow`
- Comprehensive testing with various scenarios

**F. Testing** ✅
- Unit tests for service methods
- Integration tests for database operations
- Transaction rollback scenario testing
- Async event behavior verification

## 🚀 Features

### Core Functionality
- **User Authentication & Authorization** - JWT-based secure authentication with role-based access control
- **Product Management** - Complete product catalog with stock management
- **Order Processing** - Full order lifecycle management with validation and business logic
- **Payment Integration** - Multiple payment methods with transaction tracking
- **Real-time Events** - Asynchronous event processing for order notifications
- **Advanced Reporting** - High-value order analytics and reporting

### Technical Features
- **RESTful API** - Well-structured REST endpoints with proper HTTP status codes
- **Data Validation** - Comprehensive input validation using Bean Validation
- **Database Optimization** - Optimized queries with proper indexing and batch processing
- **Transaction Management** - ACID compliance with proper rollback handling
- **Concurrent Processing** - Thread-safe operations with proper locking mechanisms
- **Comprehensive Testing** - Extensive integration and unit test coverage

## 🛠 Technology Stack

### Backend Framework
- **Spring Boot 3.5.6** - Main application framework
- **Java 21** - Latest LTS version with modern language features
- **Spring Security** - Authentication and authorization
- **Spring Data JPA** - Data persistence layer
- **Spring Web** - REST API development

### Database & Persistence
- **PostgreSQL** - Primary production database
- **H2 Database** - In-memory database for testing
- **Hibernate** - JPA implementation with advanced features
- **Database Indexing** - Optimized queries with strategic indexes

### Additional Libraries
- **Lombok** - Boilerplate code reduction
- **MapStruct** - Type-safe bean mapping
- **JWT (jjwt)** - JSON Web Token implementation
- **Bean Validation** - Input validation framework
- **Maven** - Dependency management and build tool

## 📋 Prerequisites

- **Java 21** or higher
- **Maven 3.6+**
- **PostgreSQL 12+** (for production)
- **Git** (for version control)

## 🚀 Quick Start

### 1. Clone the Repository
```bash
git clone <repository-url>
cd ecommerce
```

### 2. Database Setup
Create a PostgreSQL database:
```sql
CREATE DATABASE ecommerce;
```

### 3. Configuration
Update `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
spring.datasource.username=your_username
spring.datasource.password=your_password
```

### 4. Build and Run
```bash
# Build the project
mvn clean compile

# Run the application
mvn spring-boot:run
```

The application will start on `http://localhost:8080`

## 📚 API Documentation

### Authentication Endpoints

#### Register User
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword",
  "username": "johndoe",
  "phoneNumber": "+1234567890"
}
```

#### Login
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "securePassword"
}
```

### Order Management Endpoints

#### Create Order
```http
POST /api/v1/orders
Authorization: Bearer <jwt-token>
Content-Type: application/json

{
  "items": [
    {
      "productId": 1,
      "quantity": 2
    }
  ],
  "paymentMethod": "CARD"
}
```

**Payment Methods Available:**
- `CARD` - Credit/Debit Card payment
- `CASH` - Cash payment

#### List Orders (Paginated)
```http
GET /api/v1/orders?page=0&size=10&sort=createdAt,desc
Authorization: Bearer <jwt-token>
```

#### High-Value Orders Report
```http
GET /api/v1/orders/high-value
Authorization: Bearer <jwt-token>
```

**Response Example:**
```json
{
  "success": true,
  "data": [
    {
      "id": 1,
      "customerId": 101,
      "totalAmount": 1500.00,
      "status": "CREATED",
      "createdAt": "2024-01-15T10:30:00Z"
    }
  ]
}
```

### Native Query Implementation

The high-value orders report uses a native SQL query as required:

```java
@Query(
    value = "SELECT * FROM orders o WHERE o.total_amount > 1000",
    nativeQuery = true
)
List<OrderReportRow> findHighValueOrders();
```

**Features:**
- **Native SQL** - Direct database query for performance
- **Custom Projection** - `OrderReportRow` interface for result mapping
- **Threshold Filtering** - Orders with `total_amount > 1000`
- **Comprehensive Testing** - Multiple test scenarios including edge cases

## 🏗 Project Structure

```
src/
├── main/
│   ├── java/com/intelligent/ecommerce/
│   │   ├── config/          # Configuration classes
│   │   ├── controller/      # REST controllers
│   │   ├── dto/            # Data Transfer Objects
│   │   │   ├── auth/       # Authentication DTOs
│   │   │   ├── common/     # Common DTOs
│   │   │   ├── order/      # Order-related DTOs
│   │   │   └── payment/    # Payment DTOs
│   │   ├── entity/         # JPA entities
│   │   ├── enums/          # Enumeration types
│   │   ├── event/          # Event handling
│   │   ├── exception/      # Custom exceptions
│   │   ├── mapper/         # MapStruct mappers
│   │   ├── repository/     # Data repositories
│   │   ├── security/       # Security configuration
│   │   ├── service/        # Business logic services
│   │   ├── utilities/      # Utility classes
│   │   └── validation/     # Custom validators
│   └── resources/
│       ├── application.properties
│       └── static/
└── test/
    └── java/com/intelligent/ecommerce/
        ├── integration/    # Integration tests
        └── service/        # Unit tests
```

## 📮 Postman Collection

A complete Postman collection is available in `docs/Intelligent Task.postman_collection.json` with all API endpoints ready for testing.

### Collection Features
- **5 Pre-configured Requests** - All major API endpoints
- **Automatic Token Management** - Login/Signup automatically save JWT tokens
- **Environment Variables** - Configurable base URL and authentication
- **Ready-to-use Examples** - Sample requests with proper data

### Available Requests
1. **Login** - User authentication with auto token extraction
2. **Sign up** - User registration with auto token extraction  
3. **Create Order** - Order creation with sample items
4. **Get Orders** - Paginated order listing
5. **Get High Orders** - High-value orders report

### Setup Instructions
1. Import `docs/Intelligent Task.postman_collection.json` into Postman
2. Set the `url` variable to your server address (e.g., `http://localhost:8080`)
3. Run Login or Sign up to get authentication token
4. Use other endpoints with automatic authentication

## 🔄 Transactional Implementation

### Order Creation with Transactional Integrity

The order creation process implements full ACID compliance:

### Key Transactional Features

- **Atomic Operations** - All database changes succeed or fail together
- **Stock Locking** - `SELECT FOR UPDATE` prevents race conditions
- **Event Publishing** - Events only published after successful commit
- **Rollback Handling** - Automatic rollback on any exception
- **Concurrent Safety** - Thread-safe operations with proper locking

### Async Event Processing

```java
@Async
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void handleOrderCreated(OrderCreatedEvent event) {
    log.info("Sending confirmation email for order", event.getOrderId());
    log.info("Order has been logged to system", event.getOrderId());
}
```

**Event Features:**
- **Transaction-Safe** - Only fires after successful commit
- **Async Processing** - Non-blocking event handling
- **Mocked Services** - Email and logging simulation
- **Comprehensive Testing** - Event behavior verification

## 🧪 Testing

The project includes comprehensive testing with multiple test categories:

### Running Tests
```bash
# Run all tests
mvn test

# Run specific test class
mvn test -Dtest=OrderProcessingFlowIntegrationTest


### Test Categories
- **Unit Tests** - Individual component testing
- **Integration Tests** - End-to-end workflow testing
- **Repository Tests** - Data access layer testing
- **Service Tests** - Business logic testing
- **Transaction Tests** - Database transaction testing
- **Async Event Tests** - Event processing verification
- **Exception Handling Tests** - Error scenario testing


**Exception Features:**
- **Global Handler** - Centralized exception management
- **Custom Exceptions** - `InsufficientStockException`, `PaymentFailedException`
- **Proper HTTP Status** - Appropriate status codes for each exception
- **Transaction Rollback** - Automatic rollback on business exceptions
- **Validation Errors** - Comprehensive input validation handling

## 🔧 Configuration

### Application Properties
Key configuration options in `application.properties`:

```properties
# Database Configuration
spring.datasource.url=jdbc:postgresql://localhost:5432/ecommerce
spring.datasource.username=postgres
spring.datasource.password=1234

# JPA Configuration
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.jdbc.batch_size=50

# JWT Configuration
jwt.secret=your-secret-key
jwt.expiration-minutes=60

# Logging
logging.level.org.hibernate.engine.jdbc.batch=DEBUG
```

## 🚀 Deployment

### Production Build
```bash
mvn clean package -Pproduction
```

### Docker Deployment
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/ecommerce-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

## 📊 Database Schema

### Required Tables (As Per Task Specification)

#### 1. **orders** table
```sql
- id (Primary Key)
- customer_id (Foreign Key to users)
- total_amount (DECIMAL with precision)
- status (ENUM: CREATED, CONFIRMED, DELIVERED, CANCELLED)
- created_at (TIMESTAMP with auditing)
```

#### 2. **order_items** table
```sql
- id (Primary Key)
- order_id (Foreign Key to orders)
- product_id (Foreign Key to products)
- quantity (INTEGER)
- price (DECIMAL - calculated line total)
```

#### 3. **products** table
```sql
- id (Primary Key)
- name (VARCHAR with validation)
- stock_quantity (INTEGER with constraints)
- price (DECIMAL with precision)
```

#### 4. **payments** table
```sql
- id (Primary Key)
- order_id (Foreign Key to orders - One-to-One)
- amount (DECIMAL)
- status (VARCHAR)
- payment_method (ENUM: CARD, CASH)
```

#### 5. **users** table (Additional for authentication)
```sql
- id (Primary Key)
- email (VARCHAR - unique)
- password (VARCHAR - encrypted)
- username (VARCHAR)
- phone_number (VARCHAR)
- created_at (TIMESTAMP)
- is_verified (BOOLEAN)
- role (ENUM: ADMIN, USER)
```

### Key Relationships
- **User → Orders** (One-to-Many)
- **Order → OrderItems** (One-to-Many)
- **Order → Payment** (One-to-One)
- **OrderItem → Product** (Many-to-One)

### Database Optimizations
- **Indexes** on frequently queried columns (`total_amount`, `created_at`)
- **Batch processing** for bulk operations
- **Connection pooling** for performance
- **Transaction isolation** for data consistency

## 🔒 Security Features

- **JWT Authentication** - Stateless token-based authentication
- **Role-based Access Control** - Admin and User roles
- **Password Security** - Encrypted password storage
- **Input Validation** - Comprehensive request validation
- **SQL Injection Prevention** - Parameterized queries

## 📈 Performance Optimizations

- **Database Indexing** - Strategic indexes on frequently queried columns
- **Batch Processing** - Optimized batch operations for bulk data
- **Lazy Loading** - Efficient data fetching strategies
- **Connection Pooling** - Optimized database connections

## 📦 Task Deliverables

### 1. Code Implementation ✅

#### Order Creation with Transaction
- **Transactional Method** - `@Transactional` annotation with proper rollback
- **Stock Management** - Atomic stock updates with `SELECT FOR UPDATE`
- **Race Condition Prevention** - Proper locking mechanisms
- **Multi-item Support** - Handles multiple products in single order

#### Async Event Handling
- **OrderCreatedEvent** - Custom event class with order ID
- **TransactionalEventListener** - Event only fires after successful commit
- **Async Processing** - Non-blocking email and logging simulation
- **Event Testing** - Comprehensive event behavior verification

#### Pagination API
- **Spring Data JPA** - Native pagination support
- **Custom Response** - `PaginatedResponse` wrapper for consistent API
- **Sorting Support** - Configurable sorting by creation date
- **Performance Optimized** - Efficient database queries

#### Exception Handling
- **Global Exception Handler** - `@RestControllerAdvice` implementation
- **Custom Exceptions** - `InsufficientStockException`, `PaymentFailedException`
- **HTTP Status Codes** - Proper status codes for each exception type
- **Transaction Rollback** - Automatic rollback on business exceptions

#### Native Query for Reporting
- **High-Value Orders** - Native SQL query for orders > 1000
- **Custom Projection** - `OrderReportRow` interface for result mapping
- **Performance Optimized** - Direct database query for better performance
- **Comprehensive Testing** - Multiple test scenarios including edge cases

### 2. Test Cases ✅

#### Unit Tests
- **Service Layer Testing** - Business logic verification
- **Exception Scenarios** - Error handling validation
- **Mock Dependencies** - Isolated component testing

#### Integration Tests
- **Database Operations** - End-to-end data flow testing
- **Transaction Rollback** - Rollback scenario verification
- **Concurrent Operations** - Thread safety testing
- **Event Processing** - Async event behavior verification

#### Test Coverage
- **Order Processing Flow** - Complete order lifecycle testing
- **Stock Management** - Inventory update verification
- **Payment Processing** - Payment creation and validation
- **Error Scenarios** - Exception handling verification

## 🔄 Version History

- **v0.0.1-SNAPSHOT** - Complete E-Commerce Order Processing System
  - ✅ Pagination API implementation
  - ✅ Transactional order creation with stock management
  - ✅ Async event processing within transactions
  - ✅ Global exception handling
  - ✅ Native SQL query for high-value orders reporting
  - ✅ Comprehensive unit and integration testing
  - ✅ Transaction rollback scenario testing
  - ✅ Async event behavior verification

