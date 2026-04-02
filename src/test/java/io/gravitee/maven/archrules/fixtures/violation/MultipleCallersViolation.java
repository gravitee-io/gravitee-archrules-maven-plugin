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
package io.gravitee.maven.archrules.fixtures.violation;

import io.gravitee.maven.archrules.fixtures.FakeExecutionContext;
import org.slf4j.Logger;

/**
 * Violates the ExecutionContextLoggingArchitectureRule:
 * Multiple methods with ExecutionContext call the same private method that has ExecutionContext
 * but logs directly without using ctx.withLogger().
 */
public class MultipleCallersViolation {

    private final Logger logger;

    public MultipleCallersViolation(Logger logger) {
        this.logger = logger;
    }

    public void processFirstRequest(FakeExecutionContext ctx) {
        // First public method with ExecutionContext
        logInternal(ctx);
    }

    public void processSecondRequest(FakeExecutionContext ctx) {
        // Second public method with ExecutionContext
        logInternal(ctx);
    }

    private void logInternal(FakeExecutionContext ctx) {
        // VIOLATION: has ExecutionContext parameter but logs directly
        // Should use ctx.withLogger(logger) instead
        logger.info("Processing request");
    }
}
