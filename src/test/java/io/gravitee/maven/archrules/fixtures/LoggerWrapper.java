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
package io.gravitee.maven.archrules.fixtures;

import org.slf4j.Logger;

/**
 * Wrapper that delegates to the actual logger with context information.
 * Shared by all ExecutionContext fixtures.
 */
public class LoggerWrapper {

    private final Logger delegate;

    public LoggerWrapper(Logger delegate) {
        this.delegate = delegate;
    }

    public void info(String message) {
        delegate.info(message);
    }

    public void debug(String message) {
        delegate.debug(message);
    }

    public void error(String message) {
        delegate.error(message);
    }

    public void warn(String message) {
        delegate.warn(message);
    }

    public void trace(String message) {
        delegate.trace(message);
    }
}
