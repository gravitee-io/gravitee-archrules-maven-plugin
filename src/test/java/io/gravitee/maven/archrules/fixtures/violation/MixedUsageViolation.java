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
 * Test fixture demonstrating a mixed-usage pattern where a private method
 * is called from both methods WITH ExecutionContext and methods WITHOUT ExecutionContext.
 * This represents an ambiguous case that should generate a warning but not fail the build.
 */
public class MixedUsageViolation {

    private final Logger logger;

    public MixedUsageViolation(Logger logger) {
        this.logger = logger;
    }

    // Called WITH ExecutionContext
    public void processWithContext(FakeExecutionContext ctx) {
        logInternal();
    }

    // Called WITHOUT ExecutionContext
    public void processWithoutContext() {
        logInternal();
    }

    // Private method called by both scenarios
    private void logInternal() {
        logger.info("Processing data");
    }
}
