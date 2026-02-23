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
 * Compliant class: has ExecutionContext field but static methods can log directly
 * since they cannot access instance fields.
 */
public class CompliantWithFieldExecutionContextAndStaticMethod {

    private final Logger logger;
    private final FakeExecutionContext ctx;

    public CompliantWithFieldExecutionContextAndStaticMethod(Logger logger, FakeExecutionContext ctx) {
        this.logger = logger;
        this.ctx = ctx;
    }

    // COMPLIANT: static methods cannot access instance field ctx, so direct logging is acceptable
    public static void staticProcess(Logger logger) {
        logger.info("Processing in static method");
    }

    // COMPLIANT: instance method uses ctx.withLogger()
    public void instanceProcess() {
        ctx.withLogger(logger).info("Processing in instance method");
    }
}
