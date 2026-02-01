# AGENTS.md

This document provides essential information for AI coding agents working on the BattleJar Universe project.

## Project Overview

BattleJar Client is a project built with Java 25. The project uses a multi-module Gradle structure with the following modules:
- `api`: API definitions
- `client`: Client implementation
- `math`: Math utilities (including Vector2)

## Setup Commands

### Prerequisites
- Java 25 (as specified in build.gradle)
- Gradle (wrapper included in project)

### Initial Setup
```bash
# No dependency installation needed - Gradle handles dependencies automatically
# Verify Java version
java -version  # Should be Java 25

```

## Build Commands

### Building the Project
```bash
# Build all modules
./gradlew build

# Build specific module
./gradlew api:build
./gradlew client:build

# Clean build
./gradlew clean build
```

## Testing Instructions

### Running Tests
```bash
# Run all tests
./gradlew test

# Run tests for specific module
./gradlew api:test
./gradlew client:test

# Run tests with detailed output
./gradlew test --info
```

### Testing Framework
- **JUnit 5** (Jupiter) for test annotations and assertions
- **JUnit Jupiter Params** for parameterized tests
- **Mockito** for mocking objects
- **AssertJ** for fluent assertions

### Testing Rules
- **Mock Dependencies**: Always mock all dependencies of the class being tested. Avoid using real implementations of dependencies unless absolutely necessary.

### Adding New Tests
1. Place tests in `client/src/test/java/it/battlejar/client/` (mirroring main package structure)
2. Follow naming convention: `ClassNameTest.java`
3. Use the **given-when-then** pattern for test structure
4. Example test structure:
   ```java
   package it.battlejar.client;
   
   import org.junit.jupiter.api.Test;
   import static org.junit.jupiter.api.Assertions.assertTrue;
   
   class ExampleTest {
       @Test
       void shouldPassWhenConditionIsMet() {
           // given
           boolean condition = true;
           
           // when
           boolean result = condition;
           
           // then
           assertTrue(result, "Condition should be true");
       }
   }
   ```

### Test Best Practices
- Use parameterized tests for multiple inputs: `@ParameterizedTest` with `@MethodSource`
- Use Mockito for mocking: `mock(Class.class)` and `when().thenReturn()`
- Use AssertJ for fluent assertions: `assertThat().isEqualTo()`
- Add tests for all new functionality
- Fix any failing tests before completing the task
- **Do not add tests for functionalities that are not being actively implemented** - tests can be written to the features that already exist or features that are currently being worked on
- Do not write tests without assertions 

## Code Style

### Java Code Style
- **Indentation**: 4 spaces for Java files (see `.editorconfig`)
- **Package Structure**:
  - Main code: `client/src/main/java/it/battlejar/client/`
  - Test code: `client/src/test/java/it/battlejar/client/`
  - Packages organized by functionality (model, view, steering, logic, etc.)
- **Null Checks**: Use `Objects.requireNonNull(x)` instead of `if (x == null) throw new NullPointerException(...)`
  - Prefer static import: `requireNonNull(param, "param cannot be null")`
  - Avoid: `if (param == null) throw new NullPointerException("param cannot be null")`

### EditorConfig Rules
- Spaces for indentation (not tabs)
- LF line endings
- UTF-8 encoding
- Trim trailing whitespace (except in Markdown files)
- Insert final newline

### Lombok Usage
- Project uses Lombok to reduce boilerplate
- Common annotations: `@Log4j2`, `@Data`, `@RequiredArgsConstructor`, `@Builder`
- Use Lombok annotations instead of writing boilerplate code manually

### Logging
- Use Log4j2 via Lombok's `@Log4j2` annotation
- Example: `log.info("Message with {}", variable)`
- For test debugging: `System.out.println("[DEBUG_LOG] Your message")`

### Framework Conventions
- Use `Vector2` for 2D positions and directions (from `math` module)
- Use `Color` for identification and state

## Project Configuration

## Development Workflow

### Before Committing
1. Run all tests: `./gradlew test`
2. Ensure build succeeds: `./gradlew build`
3. Check code style compliance (follow `.editorconfig`)

### Debugging
- **Test Debugging**: Use `System.out.println("[DEBUG_LOG] ...")` or run with `--info` flag

### Common Tasks
- Generate IDE project files: `./gradlew idea` or `./gradlew eclipse`
- Clean build artifacts: `./gradlew clean`
- View test reports: Check `client/build/reports/tests/test/index.html`

## Additional Notes

- The project uses a multi-module Gradle structure
- API definitions are separated in the `api` module
- Client logic is in the `client` module
- Always run tests after making changes
- Follow the existing package structure and naming conventions
- Use Lombok annotations to reduce boilerplate code
- Maintain consistency with existing code style

