# Gravitee Architecture Rules Maven Plugin

[![Gravitee.io](https://img.shields.io/static/v1?label=Available%20at&message=Gravitee.io&color=1EC9D2)](https://download.gravitee.io/#graviteeio-apim/plugins/policies/gravitee-archrules-maven-plugin/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://github.com/gravitee-io/gravitee-archrules-maven-plugin/blob/master/LICENSE.txt)
[![Releases](https://img.shields.io/badge/semantic--release-conventional%20commits-e10079?logo=semantic-release)](https://github.com/gravitee-io/gravitee-archrules-maven-plugin/releases)
[![CircleCI](https://circleci.com/gh/gravitee-io/gravitee-archrules-maven-plugin.svg?style=svg)](https://circleci.com/gh/gravitee-io/gravitee-archrules-maven-plugin)

A Maven plugin that enforces architectural rules and logging best practices across Gravitee projects using [ArchUnit](https://www.archunit.org/).

## Overview

The Gravitee Architecture Rules Maven Plugin helps maintain code quality and consistency by automatically checking compliance with architectural rules during the build process. It focuses on enforcing logging best practices to ensure proper observability and traceability in distributed systems.

**Key benefits:**
- Catches architectural violations early in the development cycle
- Enforces consistent logging patterns across the codebase
- Improves observability by ensuring proper request context propagation
- Reduces technical debt by preventing anti-patterns

## Architecture Rules

The plugin provides two main rule checks:

### 1. Global Logging Check (`global-logging-check`)

**Purpose:** Prevents direct usage of SLF4J's `LoggerFactory` to enforce standardized logging patterns.

**Rationale:** Direct usage of `LoggerFactory.getLogger()` can lead to inconsistent logger initialization and makes it harder to enforce organization-wide logging standards.

**What it checks:**
- No classes should depend on `org.slf4j.LoggerFactory`
- Exceptions can be configured via allow-lists

**Example violation:**
```java
// ❌ Violation
import org.slf4j.LoggerFactory;

public class MyService {
    private static final Logger log = LoggerFactory.getLogger(MyService.class);
}
```

### 2. Execution Context Logging Check (`execution-context-logging-check`)

**Purpose:** Ensures that when an `ExecutionContext` is available as a method parameter, logging is done through the context-aware logger (`ctx.withLogger(log)`) instead of calling the logger directly.

**Rationale:** In request-scoped operations, using context-aware logging automatically includes correlation IDs, API keys, and other contextual information in log entries, significantly improving traceability and debugging capabilities.

**What it checks:**
- Methods that receive an `ExecutionContext` parameter should use `ctx.withLogger(log)` for logging
- Direct calls to `Logger.info()`, `Logger.debug()`, `Logger.error()`, `Logger.warn()`, or `Logger.trace()` are flagged as violations
- **Internal methods (private/protected) called from a method with `ExecutionContext` are also checked** (call graph traversal)
- Legacy `io.gravitee.gateway.api.ExecutionContext` is excluded from checks
- Additional context classes can be configured to extend the rule beyond `ExecutionContext` types (e.g., `KafkaConnectionContext`)

**Example violations:**
```java
// ❌ Violation - Direct call
public void processRequest(ExecutionContext ctx, Request request) {
    log.info("Processing request"); // Missing context information
}

//----------------------------------//

// ❌ Violation - Via internal method
public void processRequest(ExecutionContext ctx, Request request) {
    processInternal(request); // Calls private method
}

private void processInternal(Request request) {
    log.info("Processing"); // VIOLATION detected - parent has ExecutionContext
}

//----------------------------------//

// ✅ Correct - Using withLogger
public void processRequest(ExecutionContext ctx, Request request) {
    ctx.withLogger(log).info("Processing request"); // Includes correlation ID, API key, etc.
}

//----------------------------------//

// ✅ Correct - Passing ExecutionContext to internal methods
public void processRequest(ExecutionContext ctx, Request request) {
    processInternal(ctx, request);
}

private void processInternal(ExecutionContext ctx, Request request) {
    ctx.withLogger(log).info("Processing"); // Uses contextual logger
}

//----------------------------------//

// ❌ Violation - With additional context type (KafkaConnectionContext)
public void processKafkaMessage(KafkaConnectionContext ctx, Message msg) {
    log.info("Processing message"); // Missing Kafka context information
}

// ✅ Correct - Using withLogger with additional context type
public void processKafkaMessage(KafkaConnectionContext ctx, Message msg) {
    ctx.withLogger(log).info("Processing message"); // Includes Kafka connection details
}
```

**Note:** To scan additional context types like `KafkaConnectionContext`, you must configure the `additionalContextClasses` parameter (see [Advanced Configuration](#advanced-xml-configuration)).

## Configuration

### Basic XML Configuration

Add the plugin to your `pom.xml`:

```xml
<build>
    <plugins>
        <plugin>
            <groupId>io.gravitee.maven</groupId>
            <artifactId>gravitee-archrules-maven-plugin</artifactId>
            <version>1.0.0-SNAPSHOT</version>
            <executions>
                <execution>
                    <id>check-global-logging</id>
                    <goals>
                        <goal>global-logging-check</goal>
                    </goals>
                </execution>
                <execution>
                    <id>check-execution-context-logging</id>
                    <goals>
                        <goal>execution-context-logging-check</goal>
                    </goals>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build>
```

### Advanced XML Configuration

Customize rule checks with allow-lists and exclusions:

```xml
<plugin>
    <groupId>io.gravitee.maven</groupId>
    <artifactId>gravitee-archrules-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <id>check-global-logging</id>
            <goals>
                <goal>global-logging-check</goal>
            </goals>
            <configuration>
                <!-- Exclude specific classes (useful for legacy code) -->
                <allowList>
                    <allowList>com.example.LegacyService</allowList>
                    <allowList>com.example.ThirdPartyAdapter</allowList>
                </allowList>

                <!-- Exclude classes by suffix (e.g., test utilities) -->
                <allowListSuffixes>
                    <allowListSuffix>Test</allowListSuffix>
                    <allowListSuffix>TestHelper</allowListSuffix>
                </allowListSuffixes>

                <!-- Exclude entire packages from scanning -->
                <packagesToExclude>
                    <packageToExclude>com.example.legacy..</packageToExclude>
                    <packageToExclude>com.example.generated..</packageToExclude>
                </packagesToExclude>
            </configuration>
        </execution>

        <execution>
            <id>check-execution-context-logging</id>
            <goals>
                <goal>execution-context-logging-check</goal>
            </goals>
            <configuration>
                <!-- Same allow-list options as global-logging-check -->
                <allowList>
                    <allowList>com.example.SpecialHandler</allowList>
                </allowList>
                <allowListSuffixes>
                    <allowListSuffix>ConfigurationEvaluator</allowListSuffix>
                </allowListSuffixes>
                <packagesToExclude>
                    <packageToExclude>com.example.tests..</packageToExclude>
                </packagesToExclude>

                <!-- Ignore specific ExecutionContext implementations -->
                <ignoreExecutionContextClasses>
                    <ignoreExecutionContextClass>com.example.FakeExecutionContext</ignoreExecutionContextClass>
                    <ignoreExecutionContextClass>com.example.MockExecutionContext</ignoreExecutionContextClass>
                </ignoreExecutionContextClasses>

                <!-- Add additional context classes to scan (e.g., KafkaConnectionContext) -->
                <additionalContextClasses>
                    <additionalContextClass>io.gravitee.node.api.kafka.KafkaConnectionContext</additionalContextClass>
                    <additionalContextClass>com.example.CustomContext</additionalContextClass>
                </additionalContextClasses>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### CLI Configuration with `-D` Properties

All plugin parameters can be configured via command-line properties using the `gravitee.archrules.*` prefix:

```bash
# Skip architecture checks entirely
mvn verify -Dgravitee.archrules.skip=true

# Continue build even if violations are found (warnings only)
mvn verify -Dgravitee.archrules.failOnError=false

# Global logging check with allow-lists
mvn verify \
  -Dgravitee.archrules.allowList=com.example.LegacyService,com.example.ThirdPartyAdapter \
  -Dgravitee.archrules.allowListSuffixes=Test,TestHelper,ConfigurationEvaluator \
  -Dgravitee.archrules.packagesToExclude=com.example.legacy..,com.example.generated..

# Execution context logging check with specific configuration
mvn gioArchRules:execution-context-logging-check \
  -Dgravitee.archrules.allowList=com.example.SpecialHandler \
  -Dgravitee.archrules.packagesToExclude=com.example.tests.. \
  -Dgravitee.archrules.ignoreExecutionContextClasses=com.example.FakeExecutionContext,com.example.MockExecutionContext \
  -Dgravitee.archrules.additionalContextClasses=io.gravitee.node.api.kafka.KafkaConnectionContext

# Run only specific goal
mvn gioArchRules:global-logging-check
mvn gioArchRules:execution-context-logging-check

# Combine multiple parameters
mvn verify \
  -Dgravitee.archrules.allowList=com.example.LegacyClass \
  -Dgravitee.archrules.allowListSuffixes=Test \
  -Dgravitee.archrules.failOnError=false
```

**Note:** When configuring lists via CLI, use comma-separated values (e.g., `class1,class2,class3`). Maven will automatically parse them into the appropriate list format.

## Configuration Parameters

### Common Parameters (All Goals)

| Parameter | Type | Default | CLI Property | Description |
|-----------|------|---------|--------------|-------------|
| `skip` | `boolean` | `false` | `gravitee.archrules.skip` | Skip all architecture rule checks |
| `failOnError` | `boolean` | `true` | `gravitee.archrules.failOnError` | Fail the build when violations are found (if false, only warnings are logged) |
| `allowList` | `List<String>` | `[]` | `gravitee.archrules.allowList` | Fully qualified class names exempt from rule checks (comma-separated in CLI) |
| `allowListSuffixes` | `List<String>` | `[]` | `gravitee.archrules.allowListSuffixes` | Class name suffixes exempt from rule checks (e.g., "Test", "ConfigurationEvaluator"; comma-separated in CLI) |
| `packagesToExclude` | `List<String>` | `[]` | `gravitee.archrules.packagesToExclude` | Package patterns to exclude from scanning (supports ArchUnit syntax like "com.example.."; comma-separated in CLI) |

### Execution Context Logging Check Only

| Parameter | Type | Default | CLI Property | Description |
|-----------|------|---------|--------------|-------------|
| `ignoreExecutionContextClasses` | `List<String>` | `[]` | `gravitee.archrules.ignoreExecutionContextClasses` | Fully qualified class names of ExecutionContext implementations to ignore (e.g., test doubles; comma-separated in CLI) |
| `additionalContextClasses` | `List<String>` | `[]` | `gravitee.archrules.additionalContextClasses` | Fully qualified class names of additional context types to scan (e.g., `io.gravitee.node.api.kafka.KafkaConnectionContext`; comma-separated in CLI) |

## Execution Phase

Both goals run by default during the **`verify`** phase of the Maven lifecycle. This ensures architectural compliance is checked before integration tests and deployment.

## Tips and Best Practices

- **Start small:** Begin by running the checks on new modules or packages, then gradually expand coverage
- **Use allow-lists wisely:** Allow-lists are useful for legacy code, but avoid overusing them—they can hide technical debt
- **Leverage suffixes:** Use `allowListSuffixes` for patterns like test classes, generated code, or configuration evaluators
- **Package exclusions:** Use ArchUnit's `..` notation (e.g., `com.example.legacy..`) to exclude entire package trees
- **CI/CD integration:** These checks run automatically during `mvn verify` or `mvn install`, ensuring violations are caught before merge
- **Incremental adoption:** If you have a large legacy codebase, consider creating separate executions with different configurations for new vs. legacy code

## Troubleshooting

**Q: The plugin fails but I don't see any violation details**
A: Check the Maven output carefully—ArchUnit prints detailed violation reports including file locations and line numbers.

**Q: Can I run the checks on test code?**
A: By default, the plugin scans `target/classes` (main code). To scan test classes, you would need to configure `outputDirectory` to point to `target/test-classes`.

**Q: How do I temporarily disable the checks?**
A: Use `-Dgravitee.archrules.skip=true` or add `<skip>true</skip>` to the plugin configuration.

**Q: What's the difference between `allowList` and `ignoreExecutionContextClasses`?**
A: `allowList` exempts classes from **all** rule checks in that goal, while `ignoreExecutionContextClasses` specifically excludes certain types from being considered as "ExecutionContext" (useful for test doubles).

## Requirements

- **Java:** 21+
- **Maven:** 3.9.6+
- **ArchUnit:** 1.2.1

## License

Copyright © 2015 The Gravitee team (http://gravitee.io)

Licensed under the Apache License, Version 2.0
