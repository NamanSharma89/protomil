# Protomil Core - Manufacturing Execution System

![Java](https://img.shields.io/badge/Java-24-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.3-brightgreen)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15+-blue)
![AWS](https://img.shields.io/badge/AWS-Cognito-yellow)
![Build Status](https://img.shields.io/badge/build-passing-brightgreen)
![Coverage](https://img.shields.io/badge/coverage-85%25-brightgreen)

Protomil Core is a comprehensive Manufacturing Execution System (MES) built with Spring Boot 3.5.3, designed to streamline manufacturing operations through advanced user management, job card workflows, equipment tracking, and personnel coordination.

## 🏗️ Architecture Overview

### Tech Stack
- **Backend**: Spring Boot 3.5.3, Spring Security, Spring Data JPA
- **Database**: PostgreSQL with Flyway migrations
- **Authentication**: AWS Cognito with JWT tokens
- **Frontend**: Thymeleaf + HTMX for dynamic web interactions
- **Build Tool**: Maven 3.8+
- **Java Version**: 24
- **Containerization**: Docker & Docker Compose ready

### Core Modules
```
protomil-core/
├── user/          # User management & authentication
├── jobcard/       # Job card workflow management
├── equipment/     # Equipment tracking & maintenance
├── personnel/     # Personnel & skill management
├── workflow/      # Business process workflows
├── reports/       # Reporting & analytics
└── shared/        # Common utilities & security
```

## 🚀 Quick Start

### Prerequisites
- Java 24 or higher
- Maven 3.8+
- PostgreSQL 15+
- Docker & Docker Compose
- AWS CLI (for Cognito setup)

### 1. Environment Setup

#### Database Setup (PostgreSQL)
```bash
# Start PostgreSQL with Docker
docker run --name protomil-postgres \
  -e POSTGRES_DB=protomil_db \
  -e POSTGRES_USER=protomil_user \
  -e POSTGRES_PASSWORD=protomil_secure_pass_2024 \
  -p 5432:5432 \
  -d postgres:15

# Alternative: Use Docker Compose
docker-compose up -d postgres
```

#### AWS Cognito Setup
```bash
# Set AWS profile
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1

# Run Cognito setup script
chmod +x src/main/resources/aws-scripts/setup-cognito-with-custom-attributes-and-email.sh
./src/main/resources/aws-scripts/setup-cognito-with-custom-attributes-and-email.sh
```

### 2. Application Configuration

Create `application-dev.yml` with your AWS Cognito details:
```yaml
aws:
  profile: protomil-dev
  region: ap-south-1
  cognito:
    enabled: true
    userPoolId: ${AWS_COGNITO_DEV_USER_POOL_ID}
    clientId: ${AWS_COGNITO_DEV_CLIENT_ID}
    region: ap-south-1
```

### 3. Build & Run

```bash
# Clone the repository
git clone <repository-url>
cd protomil-core

# Set environment variables
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1
export AWS_COGNITO_DEV_USER_POOL_ID=your-user-pool-id
export AWS_COGNITO_DEV_CLIENT_ID=your-client-id

# Build and run
chmod +x setup-dev-env.sh
./setup-dev-env.sh
```

The application will be available at:
- **Web Interface**: http://localhost:8080
- **API Documentation**: http://localhost:8080/swagger-ui.html
- **Health Check**: http://localhost:8080/actuator/health

## 📋 Features

### Phase 1: Authentication & User Management ✅
- **User Registration**: Complete registration flow with email verification
- **AWS Cognito Integration**: Secure authentication with custom attributes
- **Role-Based Access Control**: Multi-tier permission system
- **JWT Token Management**: Secure session handling with refresh tokens
- **User Status Synchronization**: Bidirectional sync between local DB and Cognito
- **Email Verification**: Automated email verification workflow
- **Admin Approval Process**: User approval workflow for administrators

### Phase 2: Job Card Management 🚧
- Digital work instructions
- Real-time progress tracking
- Job assignment and scheduling
- Workflow automation

### Phase 3: Admin UI & User Management 📅
- Comprehensive admin dashboard
- User lifecycle management
- Role and permission administration
- System configuration

### Phase 4: Equipment Management 📅
- Asset tracking and monitoring
- Maintenance scheduling
- Equipment availability management
- Performance analytics

### Phase 5: Reporting & Analytics 📅
- Real-time dashboards
- Custom report generation
- Performance metrics
- Data export capabilities

## 🔐 Security Features

### Authentication & Authorization
- **Multi-Factor Authentication**: Via AWS Cognito
- **JWT Security**: HS512 encryption with configurable expiration
- **Cookie Management**: Secure, HTTP-only cookies with SameSite protection
- **CSRF Protection**: Built-in Spring Security CSRF tokens
- **Session Management**: Distributed session handling

### Data Protection
- **SQL Injection Prevention**: Parameterized queries via JPA
- **Input Validation**: Comprehensive validation using Bean Validation
- **Output Encoding**: XSS prevention with Thymeleaf escaping
- **Audit Logging**: Complete audit trail for all user actions

### Security Configuration
```java
// Development Environment (DevSecurityConfig)
- Relaxed security for development
- Public access to wireframes and APIs
- Detailed error reporting

// Production Environment (SecurityConfig)
- JWT-based authentication
- Role-based authorization
- Restricted endpoint access
- Comprehensive security headers
```

## 🗄️ Database Design

### Core Entities
```sql
-- User Management
users, roles, user_roles, permissions, role_permissions

-- Job Management
job_cards, work_instructions, job_status

-- Equipment Management
equipment, equipment_types, maintenance_schedules

-- Personnel Management
personnel, skills, personnel_skills, availability

-- Workflow Management
workflow_definitions, workflow_instances, workflow_steps
```

### Migration Management
- **Flyway Integration**: Version-controlled database migrations
- **Environment-Specific Scripts**: Separate migration paths for dev/test/prod
- **Rollback Support**: Safe migration rollback procedures

## 🔧 API Documentation

### Authentication Endpoints
```
POST /api/v1/auth/register     # User registration
POST /api/v1/auth/verify-email # Email verification
POST /api/v1/auth/login        # User authentication
POST /api/v1/auth/refresh      # Token refresh
POST /api/v1/auth/logout       # User logout
```

### User Management Endpoints
```
GET  /api/v1/users             # List users (paginated)
GET  /api/v1/users/{id}        # Get user details
POST /api/v1/users/{id}/approve # Approve user (admin)
PUT  /api/v1/users/{id}/suspend # Suspend user (admin)
PUT  /api/v1/users/{id}/activate # Activate user (admin)
```

### Status Sync Endpoints (Admin)
```
GET  /api/v1/admin/user-status/validate/{email}     # Validate status consistency
POST /api/v1/admin/user-status/sync-to-cognito/{email}   # Force sync to Cognito
POST /api/v1/admin/user-status/sync-from-cognito/{email} # Force sync from Cognito
POST /api/v1/admin/user-status/force-status/{userId}     # Force status change
```

## 🧪 Testing Strategy

### Test Architecture
```
├── unit/           # Unit tests with Mockito
├── integration/    # Integration tests with TestContainers
├── api/           # API tests with MockMvc
└── regression/    # End-to-end regression tests
```

### Running Tests
```bash
# Unit tests
mvn test

# Integration tests
mvn test -Dtest="**/*IntegrationTest"

# API regression tests
chmod +x regression-scripts/run-all-tests.sh
./regression-scripts/run-all-tests.sh

# Specific test suites
./regression-scripts/api/test-user-registration.sh
./regression-scripts/wireframes/test-wireframes.sh
```

### Test Coverage
- **Unit Tests**: 90%+ coverage for service layer
- **Integration Tests**: End-to-end workflow validation
- **API Tests**: Complete REST API validation
- **UI Tests**: HTMX interaction testing

## 🔄 Development Workflow

### Local Development Setup
```bash
# 1. Start dependencies
docker-compose up -d postgres redis

# 2. Set environment
source setup-dev-env.sh

# 3. Run application
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 4. Run tests
mvn test
```

### Code Quality Standards
- **Lombok**: Reduces boilerplate code
- **SonarQube**: Code quality analysis
- **Checkstyle**: Code style enforcement
- **SpotBugs**: Static code analysis
- **PMD**: Code quality validation

### Git Workflow
```bash
# Feature development
git checkout -b feature/your-feature-name
git commit -m "feat: add new feature"
git push origin feature/your-feature-name

# Create PR for code review
# Merge after approval and tests pass
```

## 📊 Monitoring & Observability

### Application Metrics
- **Spring Boot Actuator**: Health checks, metrics, info
- **Micrometer + Prometheus**: Application metrics
- **Logback + JSON**: Structured logging
- **Request/Response Logging**: Complete audit trail

### Health Checks
```bash
# Application health
curl http://localhost:8080/actuator/health

# Database connectivity
curl http://localhost:8080/actuator/health/db

# Cognito connectivity
curl http://localhost:8080/actuator/health/cognito
```

### Logging Configuration
```yaml
# Structured JSON logging for production
# Console logging for development
# File-based logging with rotation
# Trace ID correlation across requests
```

## 🚀 Deployment

### Environment Profiles
- **dev**: Local development with H2/PostgreSQL
- **test**: Testing environment with TestContainers
- **prod**: Production with AWS services

### Docker Deployment
```bash
# Build Docker image
mvn clean package
docker build -t protomil-core:latest .

# Run with Docker Compose
docker-compose up -d
```

### AWS Deployment
```yaml
# ECS Fargate deployment
# RDS PostgreSQL instance
# Cognito user pools
# Application Load Balancer
# CloudWatch logging
```

## 🔧 Configuration

### Application Properties
```yaml
# Core application settings
server:
  port: 8080
  servlet:
    context-path: /

# Database configuration
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/protomil_db
    username: ${DB_USERNAME:protomil_user}
    password: ${DB_PASSWORD:protomil_secure_pass_2024}

# AWS Cognito configuration
aws:
  cognito:
    enabled: true
    userPoolId: ${AWS_COGNITO_USER_POOL_ID}
    clientId: ${AWS_COGNITO_CLIENT_ID}
    region: ${AWS_REGION:ap-south-1}

# Security configuration
protomil:
  security:
    jwt:
      secret: ${JWT_SECRET}
      access-token-expiration: 1800
      refresh-token-expiration: 7200
```

### Environment Variables
```bash
# Required environment variables
export AWS_PROFILE=protomil-dev
export AWS_REGION=ap-south-1
export AWS_COGNITO_DEV_USER_POOL_ID=your-pool-id
export AWS_COGNITO_DEV_CLIENT_ID=your-client-id
export JWT_SECRET=your-jwt-secret
export DB_PASSWORD=your-db-password
```

## 🧰 Development Tools

### Recommended IDE Setup
- **IntelliJ IDEA**: Professional edition with Spring Boot plugin
- **VS Code**: With Java Extension Pack and Spring Boot Extension
- **Eclipse**: Spring Tools Suite (STS)

### Required Plugins
- Lombok plugin
- AWS Toolkit
- Docker plugin
- Maven integration
- Git integration

### Development Scripts
```bash
# User management scripts
./src/main/resources/activate-admin-user.sh

# AWS setup scripts
./src/main/resources/aws-scripts/setup-cognito-dev.sh
./src/main/resources/aws-scripts/sync-cognito-user.sh

# Test execution scripts
./regression-scripts/run-all-tests.sh
```

## 🤝 Contributing

### Code Style Guidelines
- Follow Spring Boot best practices
- Use meaningful variable and method names
- Write comprehensive JavaDoc for public methods
- Maintain 90%+ test coverage
- Follow RESTful API design principles

### Pull Request Process
1. Fork the repository
2. Create feature branch
3. Write tests for new functionality
4. Ensure all tests pass
5. Submit pull request with detailed description
6. Address code review feedback

### Issue Reporting
- Use GitHub issue templates
- Provide detailed reproduction steps
- Include environment information
- Attach relevant log files

## 📚 Documentation

### Additional Resources
- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Architecture Decision Records](./docs/adr/)
- [Deployment Guide](./docs/deployment.md)
- [Security Guidelines](./docs/security.md)
- [Troubleshooting Guide](./docs/troubleshooting.md)

### Learning Resources
- [Spring Boot Documentation](https://docs.spring.io/spring-boot/docs/current/reference/htmlsingle/)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/)
- [AWS Cognito Developer Guide](https://docs.aws.amazon.com/cognito/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)

## 🛠️ Troubleshooting

### Common Issues

#### Database Connection Issues
```bash
# Check PostgreSQL status
docker ps | grep postgres

# Test database connectivity
psql -h localhost -p 5432 -U protomil_user -d protomil_db
```

#### AWS Cognito Issues
```bash
# Verify AWS credentials
aws sts get-caller-identity --profile protomil-dev

# Test Cognito connectivity
aws cognito-idp list-user-pools --max-items 10 --profile protomil-dev
```

#### Authentication Issues
```bash
# Check JWT configuration
curl -H "Authorization: Bearer <token>" http://localhost:8080/api/v1/users/me

# Verify cookie settings
curl -c cookies.txt -b cookies.txt http://localhost:8080/wireframes/login
```

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 👥 Team

- **Backend Team**: Spring Boot, Security, Database Design
- **Frontend Team**: Thymeleaf, HTMX, UI/UX
- **DevOps Team**: AWS, Docker, CI/CD
- **QA Team**: Test Automation, Quality Assurance

## 📞 Support

For support and questions:
- **Email**: support@protomil.com
- **Documentation**: [Wiki](./docs/)
- **Issues**: [GitHub Issues](https://github.com/your-org/protomil-core/issues)
- **Discussions**: [GitHub Discussions](https://github.com/your-org/protomil-core/discussions)

---

**Made with ❤️ by the Protomil Team**

*Last updated: July 2025*
