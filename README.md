# Figmine Backend

This is the backend for the Figmine (Figma mobile app clone) project.

## Tech Stack
- Java 17+
- Spring Boot
- PostgreSQL
- Maven

## Features
- User authentication (JWT)
- Onboarding tracking
- User profile management
- Project and file management

## How to Run
1. Configure your PostgreSQL database in `src/main/resources/application.properties`.
2. Build and run with Maven:
   ```sh
   mvn spring-boot:run
   ```

## API Endpoints
- `/api/auth/signup` - Register new user
- `/api/auth/login` - Login (returns JWT)
- `/api/users/me` - Get current user profile
- `/api/users/me` (PUT) - Update profile
- `/api/onboarding/complete` - Mark onboarding as complete
- `/api/projects` - CRUD for projects/files

## Folder Structure
- `controller/`: REST controllers
- `service/`: Business logic
- `repository/`: Spring Data JPA repositories
- `model/`: Entity classes
- `dto/`: Data transfer objects
- `security/`: JWT security config
- `config/`: CORS, etc.

---

## Important: Lombok and Your IDE
This project uses Lombok annotations (like `@Data`, `@Builder`) to auto-generate getters, setters, and constructors. If you see errors like “cannot resolve symbol getEmail”, it means your IDE is not processing Lombok annotations.

**To fix this:**
- Install the Lombok plugin for your IDE (IntelliJ, Eclipse, VS Code, etc.)
- Enable annotation processing:
  - **IntelliJ:** File > Settings > Build, Execution, Deployment > Compiler > Annotation Processors > Enable annotation processing
  - **Eclipse:** Install Lombok and enable annotation processing in project properties

Once enabled, all `getEmail()`, `getName()`, etc., will work without errors.

For more details, see the code and comments in each file.
