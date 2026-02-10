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

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Formats logger violation messages for reporting.
 */
public class ViolationMessageFormatter {

    private ViolationMessageFormatter() {
        // Utility class
    }

    /**
     * Formats a logger violation into a human-readable message.
     *
     * @param violation the violation to format
     * @return formatted message string
     */
    public static String format(LoggerViolation violation) {
        ViolationKey key = violation.key();
        Set<String> callers = violation.callers();
        String className = key.javaClass().getSimpleName();
        String methodName = key.methodName();
        String logMethod = key.logMethod();

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
                .sorted()
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
}
