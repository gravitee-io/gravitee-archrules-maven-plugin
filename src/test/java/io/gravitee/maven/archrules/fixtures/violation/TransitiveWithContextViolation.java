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
 * VIOLATION: private method with ctx + withLogger calls another private method that logs directly.
 * This tests the fix for the flaw where filtering out methods with ctx+withLogger
 * would miss violations in methods they call.
 */
public class TransitiveWithContextViolation {

    private final Logger log;

    public TransitiveWithContextViolation(Logger log) {
        this.log = log;
    }

    public void processWithContext(FakeExecutionContext ctx) {
        privateMethodWithContext(ctx);
    }

    private void privateMethodWithContext(FakeExecutionContext ctx) {
        // This method has ctx and uses withLogger, so its direct logging is OK
        ctx.withLogger(log).info("Starting process");
        // But it calls another method that logs directly - VIOLATION!
        helperWithoutContext();
    }

    private void helperWithoutContext() {
        // VIOLATION: this method logs directly without context
        log.warn("Helper logging without context");
    }
}
