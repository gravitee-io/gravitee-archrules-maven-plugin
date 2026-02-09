/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.maven.archrules.rules;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static java.util.Collections.emptyList;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents an architecture rule to enforce context-aware logging practices when an
 * {@code ExecutionContext} is available. The rule scans compiled classes within a specified
 * output directory and verifies that methods receiving an {@code ExecutionContext} parameter
 * use {@code ctx.withLogger(log)} instead of calling {@code Logger} methods directly.
 *
 * This ensures that request context details (correlation ID, API key, etc.) are automatically
 * included in log entries, improving traceability and debugging capabilities.
 *
 * A type is considered an {@code ExecutionContext} if:
 * - Its fully qualified name contains "ExecutionContext"
 * - It does NOT belong to the legacy {@code io.gravitee.gateway.api} package
 * - It is NOT explicitly listed in the ignored execution context classes
 *
 * The rule can be customized using the nested {@link Builder} class.
 * Customizations include:
 * - MANDATORY: Specifying the output directory containing compiled classes to analyze.
 * - Allow-listing specific classes by their fully qualified names.
 * - Allow-listing classes based on their name suffixes.
 * - Excluding certain packages from the analysis.
 * - Ignoring specific {@code ExecutionContext} implementations that should not trigger the rule.
 *
 * Use the {@link Builder} to configure and execute the rule check.
 *
 * @see GlobalLoggingArchitectureRule
 */
public class ExecutionContextLoggingArchitectureRule {

    private static final String WITH_LOGGER_METHOD_NAME = "withLogger";
    private static final Set<String> LOG_METHODS = Set.of("info", "debug", "error", "warn", "trace");

    private final Set<String> allowList;
    private final Set<String> allowListSuffixes;
    private final String[] packagesToExclude;
    private final Set<String> ignoreExecutionContextClasses;
    private final Path outputDirectory;

    private ExecutionContextLoggingArchitectureRule(
        Set<String> allowList,
        Set<String> allowListSuffixes,
        Set<String> packagesToExclude,
        Set<String> ignoreExecutionContextClasses,
        Path outputDirectory
    ) {
        this.allowList = allowList;
        this.allowListSuffixes = allowListSuffixes;
        this.packagesToExclude = packagesToExclude.toArray(new String[0]);
        this.ignoreExecutionContextClasses = ignoreExecutionContextClasses;
        this.outputDirectory = outputDirectory;
    }

    public static Builder configure() {
        return new Builder();
    }

    public static final class Builder {

        private final Set<String> packagesToExclude = new HashSet<>();
        private final Set<String> allowList = new HashSet<>();
        private final Set<String> allowListSuffixes = new HashSet<>();
        private final Set<String> ignoreExecutionContextClasses = new HashSet<>();
        private Path outputDirectory;

        private Builder() {}

        /**
         * Adds a collection of class names to the allow-list. Classes in this allow-list are exempt
         * from the rules executed by this builder (useful for known legacy hotspots).
         *
         * @param allowList fully qualified class names to allow (null is ignored)
         * @return this builder
         */
        public Builder allowIn(Collection<String> allowList) {
            if (allowList != null && !allowList.isEmpty()) {
                this.allowList.addAll(allowList);
            }
            return this;
        }

        /**
         * Adds a collection of class name suffixes to the allow-list. Classes whose simple name
         * ends with any of these suffixes are exempt from the rules (e.g., "ConfigurationEvaluator", "Test").
         *
         * @param allowListSuffixes simple name suffixes to allow (null is ignored)
         * @return this builder
         */
        public Builder allowInSuffixes(Collection<String> allowListSuffixes) {
            if (allowListSuffixes != null && !allowListSuffixes.isEmpty()) {
                this.allowListSuffixes.addAll(allowListSuffixes);
            }
            return this;
        }

        /**
         * Defines the packages to exclude from the scan. Accepts ArchUnit pattern syntax (e.g. {@code "io.gravitee.node.reactive.api.."}).
         *
         * @param packagesToExclude one or more package patterns
         * @return this builder
         */
        public Builder excludePackages(Collection<String> packagesToExclude) {
            if (packagesToExclude != null && !packagesToExclude.isEmpty()) {
                this.packagesToExclude.addAll(packagesToExclude);
            }
            return this;
        }

        /**
         * Sets the output directory containing compiled classes to scan (typically target/classes).
         *
         * @param outputDirectory the path to the output directory
         * @return this builder
         */
        public Builder withOutputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        /**
         * Adds a collection of class names to be ignored during execution context evaluation.
         * Classes in this collection are excluded from relevant processing by this builder.
         *
         * @param ignoreExecutionContextClasses a collection of fully qualified class names to ignore (null values are ignored)
         * @return this builder
         */
        public Builder ignoreExecutionContextClass(Collection<String> ignoreExecutionContextClasses) {
            if (ignoreExecutionContextClasses != null && !ignoreExecutionContextClasses.isEmpty()) {
                this.ignoreExecutionContextClasses.addAll(ignoreExecutionContextClasses);
            }
            return this;
        }

        /**
         * Runs architecture rule check
         */
        public void check() {
            var rule = new ExecutionContextLoggingArchitectureRule(
                this.allowList,
                this.allowListSuffixes,
                this.packagesToExclude,
                this.ignoreExecutionContextClasses,
                this.outputDirectory
            );
            rule.check();
        }
    }

    /**
     * Checks the rule: use context-aware logger when ExecutionContext is available.
     */
    private void check() {
        JavaClasses importedClasses = importClasses();

        // If no class to analyze, then we can skip the rule.
        if (importedClasses.isEmpty()) {
            return;
        }

        classes()
            .that()
            .resideOutsideOfPackages(packagesToExclude)
            .should(notCallLoggerDirectlyWhenContextIsAvailable())
            .check(importedClasses);
    }

    private ArchCondition<? super JavaClass> notCallLoggerDirectlyWhenContextIsAvailable() {
        return new ArchCondition<>("not call Logger directly when ExecutionContext is available (use ctx.withLogger(log) instead)") {
            @Override
            public void check(JavaClass javaClass, ConditionEvents events) {
                if (isAllowListed(javaClass)) {
                    return;
                }

                // Map: ViolationKey -> Set of caller method names
                Map<ViolationKey, Set<String>> violationsByMethod = new HashMap<>();

                for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
                    boolean hasExecutionContextInParams = codeUnit
                        .getParameters()
                        .stream()
                        .anyMatch(javaParameter -> isExecutionContextType(javaParameter));

                    if (hasExecutionContextInParams) {
                        boolean usesWithLoggerMethod = codeUnit
                            .getMethodCallsFromSelf()
                            .stream()
                            .anyMatch(call -> WITH_LOGGER_METHOD_NAME.equals(call.getTarget().getName()));

                        if (usesWithLoggerMethod) {
                            continue;
                        }

                        // Collect methods to check: current method + called internal (private/protected) methods
                        Set<JavaCodeUnit> methodsToCheck = new HashSet<>();
                        methodsToCheck.add(codeUnit);
                        methodsToCheck.addAll(collectCalledInternalMethods(codeUnit, javaClass));

                        // Check for direct logger calls in all collected methods
                        for (JavaCodeUnit methodToCheck : methodsToCheck) {
                            // Check if this method has ExecutionContext parameter and uses withLogger
                            boolean hasCtxInMethod = methodToCheck
                                .getParameters()
                                .stream()
                                .anyMatch(javaParameter -> isExecutionContextType(javaParameter));

                            boolean usesWithLoggerInMethod = methodToCheck
                                .getMethodCallsFromSelf()
                                .stream()
                                .anyMatch(call -> WITH_LOGGER_METHOD_NAME.equals(call.getTarget().getName()));

                            // Skip logging checks ONLY if method has ctx AND uses withLogger
                            boolean shouldSkipLoggingCheck = hasCtxInMethod && usesWithLoggerInMethod;

                            if (!shouldSkipLoggingCheck) {
                                methodToCheck
                                    .getMethodCallsFromSelf()
                                    .stream()
                                    .filter(call -> isLoggerType(call.getTargetOwner()))
                                    .filter(call -> LOG_METHODS.contains(call.getTarget().getName()))
                                    .forEach(call -> {
                                        ViolationKey key = new ViolationKey(javaClass, methodToCheck.getName(), call.getTarget().getName());
                                        violationsByMethod.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(codeUnit.getName());
                                    });
                            }
                        }
                    }
                }

                // Generate one event per violation with grouped callers
                violationsByMethod.forEach((key, callers) -> {
                    String message = buildViolationMessage(key, callers);
                    events.add(SimpleConditionEvent.violated(key.javaClass, message));
                });
            }
        };
    }

    private String buildViolationMessage(ViolationKey key, Set<String> callers) {
        String className = key.javaClass.getSimpleName();
        String methodName = key.methodName;
        String logMethod = key.logMethod;

        if (callers.size() == 1 && callers.iterator().next().equals(methodName)) {
            // Direct call - no delegation
            return String.format(
                "[%s.%s] calls Logger.%s(), use ctx.withLogger(log).%s(...) instead",
                className,
                methodName,
                logMethod,
                logMethod
            );
        } else {
            // Called by one or more methods
            String callersList = callers
                .stream()
                .filter(caller -> !caller.equals(methodName))
                .map(caller -> "  - " + caller)
                .collect(Collectors.joining("\n"));

            return String.format(
                "[%s.%s] calls Logger.%s(), use ctx.withLogger(log).%s(...) instead. Called by:\n%s",
                className,
                methodName,
                logMethod,
                logMethod,
                callersList
            );
        }
    }

    private static class ViolationKey {

        private final JavaClass javaClass;
        private final String methodName;
        private final String logMethod;

        ViolationKey(JavaClass javaClass, String methodName, String logMethod) {
            this.javaClass = javaClass;
            this.methodName = methodName;
            this.logMethod = logMethod;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ViolationKey that = (ViolationKey) o;
            return (javaClass.equals(that.javaClass) && methodName.equals(that.methodName) && logMethod.equals(that.logMethod));
        }

        @Override
        public int hashCode() {
            int result = javaClass.hashCode();
            result = 31 * result + methodName.hashCode();
            result = 31 * result + logMethod.hashCode();
            return result;
        }
    }

    private JavaClasses importClasses() {
        if (this.outputDirectory == null) {
            throw new IllegalStateException("Output directory is not configured. Call withOutputDirectory(Path) before running checks.");
        }

        // If the output directory does not exist, we can't import classes'.
        // Using an empty list allows executing the plugin on parent modules, with no code.
        if (!Files.exists(this.outputDirectory)) {
            return new ClassFileImporter().importPaths(emptyList());
        }

        return new ClassFileImporter().importPath(this.outputDirectory);
    }

    private boolean isAllowListed(JavaClass javaClass) {
        String className = javaClass.getName();
        if (allowList.contains(className)) {
            return true;
        }
        String simpleName = javaClass.getSimpleName();
        return allowListSuffixes.stream().anyMatch(simpleName::endsWith);
    }

    private boolean isExecutionContextType(JavaParameter javaParameter) {
        String fullTypeName = javaParameter.getRawType().getName();
        return (
            isExecutionContextType(fullTypeName) && isNotLegacyExecutionContext(fullTypeName) && isNotIgnoredExecutionContext(fullTypeName)
        );
    }

    private static boolean isNotLegacyExecutionContext(String fullTypeName) {
        return !fullTypeName.contains("io.gravitee.gateway.api");
    }

    private boolean isNotIgnoredExecutionContext(String fullTypeName) {
        return ignoreExecutionContextClasses.stream().noneMatch(fullTypeName::equals);
    }

    private static boolean isExecutionContextType(String fullTypeName) {
        return fullTypeName.contains("ExecutionContext");
    }

    private static boolean isLoggerType(JavaClass targetOwner) {
        return targetOwner.isAssignableTo("org.slf4j.Logger");
    }

    /**
     * Checks if a method is private or protected (non-public internal method).
     */
    private static boolean isNonPublicMethod(JavaCodeUnit codeUnit) {
        Set<JavaModifier> modifiers = codeUnit.getModifiers();
        return modifiers.contains(JavaModifier.PRIVATE) || modifiers.contains(JavaModifier.PROTECTED);
    }

    /**
     * Collects all private and protected methods of the same class that are called
     * (directly or transitively) from the given code unit.
     * This enables detection of logging violations in delegated internal methods.
     *
     * @param origin    the method from which to start the call graph traversal
     * @param javaClass the class containing the methods
     * @return set of internal methods reachable from the origin method
     */
    private Set<JavaCodeUnit> collectCalledInternalMethods(JavaCodeUnit origin, JavaClass javaClass) {
        Set<JavaCodeUnit> visited = new HashSet<>();
        Deque<JavaCodeUnit> toVisit = new ArrayDeque<>();
        toVisit.add(origin);

        while (!toVisit.isEmpty()) {
            JavaCodeUnit current = toVisit.poll();
            current
                .getMethodCallsFromSelf()
                .stream()
                .flatMap(call -> call.getTarget().resolveMember().stream())
                .filter(target -> belongsToClass(target, javaClass))
                .filter(ExecutionContextLoggingArchitectureRule::isNonPublicMethod)
                .filter(target -> isNotYetVisited(target, visited))
                .forEach(target -> {
                    visited.add(target);
                    toVisit.add(target); // Continue traversal recursively
                });
        }
        return visited;
    }

    private static boolean belongsToClass(JavaCodeUnit codeUnit, JavaClass javaClass) {
        return codeUnit.getOwner().equals(javaClass);
    }

    private static boolean isNotYetVisited(JavaCodeUnit codeUnit, Set<JavaCodeUnit> visited) {
        return !visited.contains(codeUnit);
    }
}
