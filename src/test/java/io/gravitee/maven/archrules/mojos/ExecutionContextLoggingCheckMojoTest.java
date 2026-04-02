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
package io.gravitee.maven.archrules.mojos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.List;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class ExecutionContextLoggingCheckMojoTest {

    public static final String TARGET_TEST_CLASSES = "target/test-classes";

    @Mock
    private Log log;

    private final ExecutionContextLoggingCheckMojo cut = new ExecutionContextLoggingCheckMojo();

    @BeforeEach
    void setUp() {
        cut.setLog(log);
        cut.setSkip(false);
        cut.setFailOnError(true);
        cut.setOutputDirectory(new File(TARGET_TEST_CLASSES));
    }

    @Nested
    class SkipArchRule {

        @Test
        void should_skip_execution_when_skip_is_true() {
            cut.setSkip(true);
            assertThatCode(cut::execute).doesNotThrowAnyException();
            verify(log).info("Skipping execution of ArchRulesMojo: ExecutionContextLoggingCheckMojo");
        }
    }

    @Nested
    class ArchRuleExecution {

        @Test
        void should_pass_for_compliant_classes() throws Exception {
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.violation.."));

            assertThatCode(cut::execute).doesNotThrowAnyException();
        }

        @Test
        void should_fail_for_execution_context_violation() throws Exception {
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.compliant.."));

            assertThatThrownBy(cut::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("was violated (11 times)")
                .hasMessageContaining("not call Logger directly when ExecutionContext is available");
        }

        @Test
        void shouldWarnInsteadOfFailWhenFailOnErrorIsFalse() throws Exception {
            cut.setFailOnError(false);
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.compliant.."));
            cut.setFailOnError(false);

            assertThatCode(cut::execute).doesNotThrowAnyException();
            verify(log).warn(
                argThat(
                    (String message) ->
                        message.contains("was violated (11 times)") &&
                        message.contains("not call Logger directly when ExecutionContext is available")
                )
            );
        }
    }

    @Nested
    class AllowList {

        @Test
        void should_still_fail_when_only_some_classes_are_in_allow_list() throws Exception {
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.compliant.."));
            cut.setAllowList(List.of("io.gravitee.maven.archrules.fixtures.violation.WithExecutionContextViolation"));

            assertThatThrownBy(cut::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("AnotherWithExecutionContextViolation");
        }
    }

    @Nested
    class AllowListSuffixes {

        @Test
        void should_pass_when_violation_classes_match_suffixes() throws Exception {
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.compliant.."));
            cut.setAllowListSuffixes(List.of("Violation"));

            assertThatCode(cut::execute).doesNotThrowAnyException();
        }
    }

    @Nested
    class ExcludePackages {

        @Test
        void should_exclude_packages_from_scan() throws Exception {
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.violation.."));

            assertThatCode(cut::execute).doesNotThrowAnyException();
        }
    }
}
