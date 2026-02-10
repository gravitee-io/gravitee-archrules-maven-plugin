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

/**
 * Sealed hierarchy representing violation information with different severity levels.
 */
public sealed interface ViolationInfo permits ViolationInfo.Error, ViolationInfo.Warning {
    Set<String> callers();

    /**
     * A blocking violation that should fail the build.
     */
    record Error(Set<String> callers) implements ViolationInfo {}

    /**
     * A non-blocking warning for ambiguous mixed-usage patterns.
     * Occurs when a private method is called both from methods with ExecutionContext
     * and methods without ExecutionContext, making it unclear whether to pass context.
     */
    record Warning(Set<String> callers) implements ViolationInfo {}
}
