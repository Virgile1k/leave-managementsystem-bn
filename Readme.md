# Leave Management System

## Overview
The Leave Management System is a comprehensive solution built with Spring Boot for managing employee time-off requests. It provides role-based access control with distinct permissions for administrators, managers, and staff members.

## Features

- Role-based user management (Admin, Manager, Staff)
- Department-based organization
- Leave request submission and approval workflow
- Calendar integration with Microsoft Outlook
- Email notifications
- Document upload capabilities
- Secure authentication with JWT
- Microsoft Single Sign-On (SSO) integration

## Technology Stack

- **Backend**: Spring Boot, Java
- **Database**: PostgreSQL
- **Security**: Spring Security, JWT
- **Email**: SMTP Integration
- **File Storage**: AWS S3
- **Calendar**: Microsoft Graph API
- **Documentation**: Swagger/OpenAPI

## Setup Instructions

### Prerequisites

- Java 11 or higher
- PostgreSQL 12 or higher
- Maven

### Database Configuration
The application requires a PostgreSQL database. By default, it connects to:
```
jdbc:postgresql://localhost:5433/leave_management
```

### Configuration
The application uses environment variables for configuration. Key variables include:

#### Database Configuration
```
DATABASE_URL=jdbc:postgresql://localhost:5433/leave_management
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=your_password
```

#### JWT Configuration
```
JWT_SECRET=your_jwt_secret_key
JWT_EXPIRATION=3600000
JWT_REFRESH_EXPIRATION=604800000
```

#### Microsoft Authentication
```
MS_CLIENT_ID=your_microsoft_client_id
MS_CLIENT_SECRET=your_microsoft_client_secret
MS_TENANT_ID=your_microsoft_tenant_id
MS_REDIRECT_URI=http://localhost:5173/api/v1/auth/microsoft/callback
```

#### Email Configuration
```
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_email_password
```

#### AWS Configuration
```
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key
AWS_REGION=eu-north-1
AWS_S3_BUCKET_NAME=your_bucket_name
```

#### Azure/Microsoft Graph API
```
AZURE_TENANT_ID=your_azure_tenant_id
AZURE_CLIENT_ID=your_azure_client_id
AZURE_CLIENT_SECRET=your_azure_client_secret
AZURE_DEFAULT_USER_EMAIL=ndayambaje.virgile@techsroutine.com
```

## Default Credentials

### Admin Users
- Email: ndayambaje.virgile@techsroutine.com
    - Password: admin123
    - Role: ADMIN
- Email: ndayambajevg16bussiness@gmail.com
    - Password: password123
    - Role: ADMIN

### Department Managers
- Email: hr.manager@company.com (HR Department)
    - Password: password123
    - Role: MANAGER
- Email: tech.manager@company.com (Technology Department)
    - Password: password123
    - Role: MANAGER
- Email: finance.manager@company.com (Finance Department)
    - Password: password123
    - Role: MANAGER
- Email: marketing.manager@company.com (Marketing Department)
    - Password: password123
    - Role: MANAGER

### Staff Users
- Email: hr.employee1@company.com, hr.employee2@company.com
- Email: developer1@company.com, developer2@company.com, qa.engineer@company.com, devops@company.com
- Email: accountant1@company.com, accountant2@company.com
- Email: marketing.specialist1@company.com, marketing.specialist2@company.com
- Password for all staff users: password123
- Role: STAFF

## Running the Application
```bash
# Set environment variables or use application.properties

# Build the application
mvn clean install

# Run the application
mvn spring-boot:run
```
The application will start on port 3000 by default.

## API Documentation
Swagger UI is available at:
```
http://localhost:3000/swagger-ui.html
```

API documentation in OpenAPI format is available at:
```
http://localhost:3000/api-docs
```

## Development Notes

- The UserSeeder class automatically creates required departments and users at application startup
- Changes to the database schema are managed through Hibernate's auto DDL update feature
- For security reasons, remember to change default passwords in production environments

## License
This project is proprietary and confidential. Unauthorized copying or distribution is prohibited.