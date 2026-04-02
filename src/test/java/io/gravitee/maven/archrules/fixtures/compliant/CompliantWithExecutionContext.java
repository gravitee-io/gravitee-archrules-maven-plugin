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
package io.gravitee.maven.archrules.fixtures.compliant;

import io.gravitee.maven.archrules.fixtures.FakeExecutionContext;
import org.slf4j.Logger;

/**
 * Compliant class that properly uses ExecutionContext.withLogger() for logging.
 * This is the correct way to log when ExecutionContext is available.
 */
public class CompliantWithExecutionContext {

    private final Logger logger;

    public CompliantWithExecutionContext(Logger logger) {
        this.logger = logger;
    }

    public void processWithContext(FakeExecutionContext ctx) {
        // COMPLIANT: using ctx.withLogger() to include context in logs
        ctx.withLogger(logger).info("Processing with context");
    }
}
