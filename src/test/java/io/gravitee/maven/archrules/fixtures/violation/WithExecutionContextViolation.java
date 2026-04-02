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
 * Has an ExecutionContext parameter but calls logger directly instead of using ctx.withLogger().
 */
public class WithExecutionContextViolation {

    private final Logger logger;

    public WithExecutionContextViolation(Logger logger) {
        this.logger = logger;
    }

    public void processWithContext(FakeExecutionContext ctx) {
        // VIOLATION: calling logger directly when ExecutionContext is available
        logger.info("Processing with context");
    }
}
