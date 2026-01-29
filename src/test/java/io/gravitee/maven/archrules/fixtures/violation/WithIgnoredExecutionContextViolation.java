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

import io.gravitee.maven.archrules.fixtures.IgnoredExecutionContext;
import org.slf4j.Logger;

/**
 * Uses an IgnoredExecutionContext.
 * When the context is ignored, calling logger directly should be allowed.
 */
public class WithIgnoredExecutionContextViolation {

    private final Logger logger;

    public WithIgnoredExecutionContextViolation(Logger logger) {
        this.logger = logger;
    }

    public void processWithIgnoredContext(IgnoredExecutionContext ctx) {
        // This would normally be a violation, but IgnoredExecutionContext is in the ignore list
        logger.info("Processing with ignored context");
    }
}
