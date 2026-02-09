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
package io.gravitee.maven.archrules.rules;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;
import java.util.Set;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ExecutionContextLoggingArchitectureRuleTest {

    public static final String TARGET_TEST_CLASSES = "target/test-classes";

    @Test
    void should_throw_exception_when_no_output_directory_is_configured() {
        assertThatThrownBy(() -> ExecutionContextLoggingArchitectureRule.configure().check())
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Output directory is not configured");
    }

    @Test
    void should_pass_for_compliant_classes() {
        assertThatCode(() -> {
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .excludePackages(Set.of("io.gravitee.maven.archrules.fixtures.violation.."))
                .check();
        }).doesNotThrowAnyException();
    }

    @Test
    void should_fail_for_methods_calling_logger_directly_with_execution_context() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure().withOutputDirectory(Paths.get(TARGET_TEST_CLASSES)).check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("not call Logger directly when ExecutionContext is available")
            .hasMessageContaining("was violated (9 times)");
    }

    @Test
    void should_pass_when_all_violation_classes_are_in_allow_list() {
        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .allowIn(
                    Set.of(
                        "io.gravitee.maven.archrules.fixtures.violation.WithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.AnotherWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.subpackage.SubpackageWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.WithIgnoredExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.InternalMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.ProtectedMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.TransitiveCallViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.TransitiveWithContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.MultipleCallersViolation"
                    )
                )
                .check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_violation_class_matches_suffix() {
        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .allowInSuffixes(Set.of("Violation"))
                .check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_output_directory_does_not_exist() {
        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure().withOutputDirectory(Paths.get("non-existent-directory")).check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_fail_when_excluding_only_subpackage() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .excludePackages(Set.of("io.gravitee.maven.archrules.fixtures.violation.subpackage.."))
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("not call Logger directly when ExecutionContext is available");
    }

    @Test
    void should_pass_when_ignoring_execution_context_class() {
        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .ignoreExecutionContextClass(Set.of("IgnoredExecutionContext"))
                .allowInSuffixes(Set.of("Violation"))
                .check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_fail_when_private_method_calls_logger_directly() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .allowIn(
                    Set.of(
                        "io.gravitee.maven.archrules.fixtures.violation.WithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.AnotherWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.subpackage.SubpackageWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.WithIgnoredExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.ProtectedMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.TransitiveCallViolation"
                    )
                )
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[InternalMethodViolation.processInternal]")
            .hasMessageContaining("Called by:\n  - processWithContext");
    }

    @Test
    void should_fail_when_protected_method_calls_logger_directly() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .allowIn(
                    Set.of(
                        "io.gravitee.maven.archrules.fixtures.violation.WithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.AnotherWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.subpackage.SubpackageWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.WithIgnoredExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.InternalMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.TransitiveCallViolation"
                    )
                )
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[ProtectedMethodViolation.processProtected]")
            .hasMessageContaining("Called by:\n  - processWithContext");
    }

    @Test
    void should_fail_when_transitive_call_logs_directly() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .allowIn(
                    Set.of(
                        "io.gravitee.maven.archrules.fixtures.violation.WithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.AnotherWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.subpackage.SubpackageWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.WithIgnoredExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.InternalMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.ProtectedMethodViolation"
                    )
                )
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[TransitiveCallViolation.secondInternalMethod]")
            .hasMessageContaining("Called by:\n  - processWithContext");
    }

    @Test
    void should_pass_when_compliant_class_with_internal_method_and_context() {
        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .excludePackages(Set.of("io.gravitee.maven.archrules.fixtures.violation.."))
                .check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_fail_when_private_method_with_context_calls_method_without_context() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .allowIn(
                    Set.of(
                        "io.gravitee.maven.archrules.fixtures.violation.WithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.AnotherWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.subpackage.SubpackageWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.WithIgnoredExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.InternalMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.ProtectedMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.TransitiveCallViolation"
                    )
                )
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[TransitiveWithContextViolation.helperWithoutContext]")
            .hasMessageContaining("Called by:\n  - processWithContext");
    }

    @Test
    void should_group_violations_by_method_with_multiple_callers() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .allowIn(
                    Set.of(
                        "io.gravitee.maven.archrules.fixtures.violation.WithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.AnotherWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.subpackage.SubpackageWithExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.WithIgnoredExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.InternalMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.ProtectedMethodViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.TransitiveCallViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.TransitiveWithContextViolation"
                    )
                )
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("[MultipleCallersViolation.logInternal] calls Logger.info()")
            .hasMessageContaining("Called by:\n  - processFirstRequest\n  - processSecondRequest");
    }
}
