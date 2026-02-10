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
package io.gravitee.maven.archrules.violations.executioncontext;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.domain.JavaParameter;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Collects logger violations from a Java class by analyzing method calls and ExecutionContext usage.
 * Uses a fluent builder pattern for configuration.
 */
public class ViolationCollector {

    private static final String WITH_LOGGER_METHOD_NAME = "withLogger";
    private static final Set<String> LOG_METHODS = Set.of("info", "debug", "error", "warn", "trace");

    private final JavaClass javaClass;
    private final Set<String> ignoreExecutionContextClasses;

    private ViolationCollector(JavaClass javaClass, Set<String> ignoreExecutionContextClasses) {
        this.javaClass = javaClass;
        this.ignoreExecutionContextClasses = ignoreExecutionContextClasses;
    }

    /**
     * Creates a new ViolationCollector for the given class.
     *
     * @param javaClass the class to analyze
     * @return a new collector builder
     */
    public static Builder forClass(JavaClass javaClass) {
        return new Builder(javaClass);
    }

    /**
     * Builder for ViolationCollector with fluent API.
     */
    public static class Builder {

        private final JavaClass javaClass;
        private Set<String> ignoreExecutionContextClasses = new HashSet<>();

        private Builder(JavaClass javaClass) {
            this.javaClass = javaClass;
        }

        /**
         * Specifies ExecutionContext classes to ignore during violation detection.
         *
         * @param ignoreExecutionContextClasses set of fully qualified class names to ignore
         * @return this builder
         */
        public Builder withIgnoredContexts(Set<String> ignoreExecutionContextClasses) {
            this.ignoreExecutionContextClasses = ignoreExecutionContextClasses;
            return this;
        }

        /**
         * Collects all violations in the configured class.
         *
         * @return map of violation keys to violation information (callers + mixed-usage flag)
         */
        public Map<ViolationKey, ViolationInfo> collect() {
            ViolationCollector collector = new ViolationCollector(javaClass, ignoreExecutionContextClasses);
            return collector.collectViolations();
        }
    }

    /**
     * Collects all logger violations in the configured class.
     *
     * @return map of violation keys to violation information
     */
    private Map<ViolationKey, ViolationInfo> collectViolations() {
        // First pass: identify all methods WITH ExecutionContext (potential violations)
        Map<ViolationKey, Set<String>> violationsByMethod = new HashMap<>();

        for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
            boolean hasExecutionContextInParams = codeUnit.getParameters().stream().anyMatch(this::isExecutionContextType);

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
                methodsToCheck.addAll(collectCalledInternalMethods(codeUnit));

                // Check for direct logger calls in all collected methods
                for (JavaCodeUnit methodToCheck : methodsToCheck) {
                    // Check if this method has ExecutionContext parameter and uses withLogger
                    boolean hasCtxInMethod = methodToCheck.getParameters().stream().anyMatch(this::isExecutionContextType);

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

        // Second pass: check if any violation method is also called from methods WITHOUT ExecutionContext
        Map<ViolationKey, Set<String>> callersWithoutContext = new HashMap<>();

        for (JavaCodeUnit codeUnit : javaClass.getCodeUnits()) {
            boolean hasExecutionContextInParams = codeUnit.getParameters().stream().anyMatch(this::isExecutionContextType);

            // Only check methods WITHOUT ExecutionContext
            if (!hasExecutionContextInParams) {
                // Collect internal methods called by this method
                Set<JavaCodeUnit> calledInternalMethods = collectCalledInternalMethods(codeUnit);

                // For each internal method, check if it logs (and would be in violationsByMethod)
                for (JavaCodeUnit internalMethod : calledInternalMethods) {
                    internalMethod
                        .getMethodCallsFromSelf()
                        .stream()
                        .filter(call -> isLoggerType(call.getTargetOwner()))
                        .filter(call -> LOG_METHODS.contains(call.getTarget().getName()))
                        .forEach(call -> {
                            ViolationKey key = new ViolationKey(javaClass, internalMethod.getName(), call.getTarget().getName());
                            // Only track if this is a known violation
                            if (violationsByMethod.containsKey(key)) {
                                callersWithoutContext.computeIfAbsent(key, k -> new LinkedHashSet<>()).add(codeUnit.getName());
                            }
                        });
                }
            }
        }

        // Build final result with mixed-usage detection
        // Mixed-usage: method is called WITH ctx (violation) AND also called WITHOUT ctx (legitimate)
        // This means the private method serves both contexts and it's ambiguous whether it needs ctx
        return violationsByMethod
            .entrySet()
            .stream()
            .collect(
                Collectors.toMap(Map.Entry::getKey, entry -> {
                    ViolationKey key = entry.getKey();
                    Set<String> callers = entry.getValue();
                    return isIsMixedUsage(callersWithoutContext, key)
                        ? new ViolationInfo.Warning(callers)
                        : new ViolationInfo.Error(callers);
                })
            );
    }

    /**
     * Determines if a given {@link ViolationKey} is associated with mixed usage patterns.
     * Mixed usage occurs when the key is present in the map with non-empty associated data.
     *
     * @param callersWithoutContext a map containing {@link ViolationKey} entries with sets of callers
     *                              that do not involve an execution context
     * @param key                   the {@link ViolationKey} to check for mixed usage
     * @return {@code true} if the key is present in the map and has non-empty callers;
     *         {@code false} otherwise
     */
    private static boolean isIsMixedUsage(Map<ViolationKey, Set<String>> callersWithoutContext, ViolationKey key) {
        return callersWithoutContext.containsKey(key) && !callersWithoutContext.get(key).isEmpty();
    }

    /**
     * Checks if a parameter type is an ExecutionContext.
     */
    private boolean isExecutionContextType(JavaParameter javaParameter) {
        String fullTypeName = javaParameter.getRawType().getName();
        return (
            isExecutionContextType(fullTypeName) && isNotLegacyExecutionContext(fullTypeName) && isNotIgnoredExecutionContext(fullTypeName)
        );
    }

    private static boolean isExecutionContextType(String fullTypeName) {
        return fullTypeName.contains("ExecutionContext");
    }

    private static boolean isNotLegacyExecutionContext(String fullTypeName) {
        return !fullTypeName.contains("io.gravitee.gateway.api");
    }

    private boolean isNotIgnoredExecutionContext(String fullTypeName) {
        return ignoreExecutionContextClasses.stream().noneMatch(fullTypeName::equals);
    }

    /**
     * Collects all private and protected methods of the same class that are called
     * (directly or transitively) from the given code unit.
     *
     * @param origin the method from which to start the call graph traversal
     * @return set of internal methods reachable from the origin method
     */
    private Set<JavaCodeUnit> collectCalledInternalMethods(JavaCodeUnit origin) {
        Set<JavaCodeUnit> visited = new HashSet<>();
        Deque<JavaCodeUnit> toVisit = new ArrayDeque<>();
        toVisit.add(origin);

        while (!toVisit.isEmpty()) {
            JavaCodeUnit current = toVisit.poll();
            current
                .getMethodCallsFromSelf()
                .stream()
                .flatMap(call -> call.getTarget().resolveMember().stream())
                .filter(target -> belongsToClass(target))
                .filter(ViolationCollector::isNonPublicMethod)
                .filter(target -> isNotYetVisited(target, visited))
                .forEach(target -> {
                    visited.add(target);
                    toVisit.add(target);
                });
        }
        return visited;
    }

    private static boolean isLoggerType(JavaClass targetOwner) {
        return targetOwner.isAssignableTo("org.slf4j.Logger");
    }

    private static boolean isNonPublicMethod(JavaCodeUnit codeUnit) {
        Set<JavaModifier> modifiers = codeUnit.getModifiers();
        return modifiers.contains(JavaModifier.PRIVATE) || modifiers.contains(JavaModifier.PROTECTED);
    }

    private boolean belongsToClass(JavaCodeUnit codeUnit) {
        return codeUnit.getOwner().equals(javaClass);
    }

    private static boolean isNotYetVisited(JavaCodeUnit codeUnit, Set<JavaCodeUnit> visited) {
        return !visited.contains(codeUnit);
    }
}
