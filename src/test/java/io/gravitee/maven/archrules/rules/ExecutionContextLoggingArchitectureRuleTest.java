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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
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
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .withWarningHandler(msg -> {}) // Silent handler for warnings
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("not call Logger directly when ExecutionContext is available")
            .hasMessageContaining("was violated (11 times)");
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
                        "io.gravitee.maven.archrules.fixtures.violation.MultipleCallersViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.MixedUsageViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.FieldExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.FieldExecutionContextMixedUsageViolation"
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
                .withWarningHandler(msg -> {}) // Silent handler for warnings
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
                .withWarningHandler(msg -> {}) // Silent handler for warnings
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
                .withWarningHandler(msg -> {}) // Silent handler for warnings
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
                .withWarningHandler(msg -> {}) // Silent handler for warnings
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
                .withWarningHandler(msg -> {}) // Silent handler for warnings
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
                .withWarningHandler(msg -> {}) // Silent handler for warnings
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

    @Test
    void should_emit_warning_for_mixed_usage_pattern_without_failing_build() {
        List<String> warnings = new ArrayList<>();

        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .withWarningHandler(warnings::add)
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
                        "io.gravitee.maven.archrules.fixtures.violation.MultipleCallersViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.FieldExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.FieldExecutionContextMixedUsageViolation"
                    )
                )
                .check()
        ).doesNotThrowAnyException();

        assertThat(warnings).hasSize(1).first().asString().contains("[MixedUsageViolation.logInternal] calls Logger.info()");
    }

    @Test
    void should_pass_when_additional_context_is_not_configured() {
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
                        "io.gravitee.maven.archrules.fixtures.violation.MultipleCallersViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.MixedUsageViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.FieldExecutionContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.FieldExecutionContextMixedUsageViolation"
                    )
                )
                .check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_fail_when_additional_context_class_is_configured_and_used_improperly() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .withWarningHandler(msg -> {}) // Silent handler for warnings
                .withAdditionalContextClasses(Set.of("io.gravitee.maven.archrules.fixtures.FakeKafkaConnectionContext"))
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
                        "io.gravitee.maven.archrules.fixtures.violation.MultipleCallersViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.MixedUsageViolation"
                    )
                )
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("not call Logger directly when ExecutionContext is available")
            .hasMessageContaining("[WithAdditionalContextViolation.processWithKafkaContext] calls Logger.info()");
    }

    @Test
    void should_pass_when_additional_context_violation_is_in_allow_list() {
        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .withAdditionalContextClasses(Set.of("io.gravitee.maven.archrules.fixtures.FakeKafkaConnectionContext"))
                .allowInSuffixes(Set.of("Violation"))
                .check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_fail_when_class_has_execution_context_field_and_logs_directly() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .withWarningHandler(msg -> {}) // Silent handler for warnings
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
                        "io.gravitee.maven.archrules.fixtures.violation.MultipleCallersViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.MixedUsageViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.WithAdditionalContextViolation"
                    )
                )
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("not call Logger directly when ExecutionContext is available")
            .hasMessageContaining("[FieldExecutionContextViolation.process] calls Logger.info()");
    }

    @Test
    void should_pass_when_class_has_execution_context_field_and_uses_withLogger() {
        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .excludePackages(Set.of("io.gravitee.maven.archrules.fixtures.violation.."))
                .check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_pass_when_static_method_logs_directly_even_with_execution_context_field() {
        assertThatCode(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .excludePackages(Set.of("io.gravitee.maven.archrules.fixtures.violation.."))
                .check()
        ).doesNotThrowAnyException();
    }

    @Test
    void should_fail_when_field_context_has_mixed_usage_in_same_method() {
        assertThatThrownBy(() ->
            ExecutionContextLoggingArchitectureRule.configure()
                .withOutputDirectory(Paths.get(TARGET_TEST_CLASSES))
                .withWarningHandler(msg -> {}) // Silent handler for warnings
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
                        "io.gravitee.maven.archrules.fixtures.violation.MultipleCallersViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.MixedUsageViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.WithAdditionalContextViolation",
                        "io.gravitee.maven.archrules.fixtures.violation.FieldExecutionContextViolation"
                    )
                )
                .check()
        )
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("not call Logger directly when ExecutionContext is available")
            .hasMessageContaining("[FieldExecutionContextMixedUsageViolation.processWithMixedUsage] calls Logger.debug()");
    }
}
