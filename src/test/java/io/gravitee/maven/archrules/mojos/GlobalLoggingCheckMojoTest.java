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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.util.List;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
@ExtendWith(MockitoExtension.class)
class GlobalLoggingCheckMojoTest {

    public static final String TARGET_TEST_CLASSES = "target/test-classes";

    @Mock
    private Log log;

    private final GlobalLoggingCheckMojo cut = new GlobalLoggingCheckMojo();

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
            verify(log).info("Skipping execution of ArchRulesMojo: GlobalLoggingCheckMojo");
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
        void should_fail_for_logger_factory_violation() throws Exception {
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.compliant.."));

            assertThatThrownBy(cut::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("Rule 'Classes must not depend on SLF4J's LoggerFactory' was violated (4 times)");
        }

        @Test
        void should_warn_instead_of_fail_when_fail_on_error_is_false() throws Exception {
            cut.setFailOnError(false);
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.compliant.."));
            cut.setFailOnError(false);

            assertThatCode(cut::execute).doesNotThrowAnyException();
            verify(log).warn(
                argThat((String message) ->
                    message.contains("Rule 'Classes must not depend on SLF4J's LoggerFactory' was violated (4 times)")
                )
            );
        }
    }

    @Nested
    class AllowList {

        @Test
        void should_still_fail_when_only_some_classes_are_in_allow_list() throws Exception {
            cut.setPackagesToExclude(List.of("io.gravitee.maven.archrules.fixtures.compliant.."));
            cut.setAllowList(
                List.of(
                    "io.gravitee.maven.archrules.fixtures.violation.WithLoggerFactoryViolation",
                    "io.gravitee.maven.archrules.fixtures.violation.AllowedBySuffixViolation"
                )
            );

            assertThatThrownBy(cut::execute)
                .isInstanceOf(MojoFailureException.class)
                .hasMessageContaining("AnotherWithLoggerFactoryViolation");
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
