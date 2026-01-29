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

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static java.util.Collections.emptyList;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.lang.syntax.elements.ClassesShould;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents a rule to enforce logging architecture constraints, disallowing the use
 * of SLF4J's {@code LoggerFactory} unless explicitly allowed. The rule scans compiled classes
 * within a specified output directory and applies the configured constraints.
 *
 * The rule can be customized using the nested {@link Builder} class.
 * Customizations include:
 * - MANDATORY: Specifying the output directory containing compiled classes to analyze.
 * - Allow-listing specific classes by their fully qualified names.
 * - Allow-listing classes based on their name suffixes.
 * - Excluding certain packages from the analysis.
 *
 * The rule enforces that classes, except those explicitly allowed, should not depend on
 * SLF4J's {@code LoggerFactory}.
 *
 * Use the {@link Builder} to configure and execute the rule check.
 */
public class GlobalLoggingArchitectureRule {

    private static final String SLF4J_LOGGER_FACTORY = "org.slf4j.LoggerFactory";

    private final Set<String> allowList;
    private final Set<String> allowListSuffixes;
    private final String[] packagesToExclude;
    private final Path outputDirectory;

    private GlobalLoggingArchitectureRule(
        Set<String> allowList,
        Set<String> allowListSuffixes,
        Set<String> packagesToExclude,
        Path outputDirectory
    ) {
        this.allowList = allowList;
        this.allowListSuffixes = allowListSuffixes;
        this.packagesToExclude = packagesToExclude.toArray(new String[0]);
        this.outputDirectory = outputDirectory;
    }

    public static Builder configure() {
        return new Builder();
    }

    public static final class Builder {

        private final Set<String> packagesToExclude = new HashSet<>();
        private final Set<String> allowList = new HashSet<>();
        private final Set<String> allowListSuffixes = new HashSet<>();
        private Path outputDirectory;

        private Builder() {}

        /**
         * Adds a collection of class names to the allow-list. Classes in this allow-list are exempt
         * from the rules executed by this builder (useful for known legacy hotspots).
         *
         * @param allowList fully qualified class names to allow (null is ignored)
         * @return this builder
         */
        public Builder allowIn(Collection<String> allowList) {
            if (allowList != null && !allowList.isEmpty()) {
                this.allowList.addAll(allowList);
            }
            return this;
        }

        /**
         * Adds a collection of class name suffixes to the allow-list. Classes whose simple name
         * ends with any of these suffixes are exempt from the rules (e.g., "ConfigurationEvaluator", "Test").
         *
         * @param allowListSuffixes simple name suffixes to allow (null is ignored)
         * @return this builder
         */
        public Builder allowInSuffixes(Collection<String> allowListSuffixes) {
            if (allowListSuffixes != null && !allowListSuffixes.isEmpty()) {
                this.allowListSuffixes.addAll(allowListSuffixes);
            }
            return this;
        }

        /**
         * Defines the packages to exclude from the scan. Accepts ArchUnit pattern syntax (e.g. {@code "io.gravitee.node.reactive.api.."}).
         *
         * @param packagesToExclude one or more package patterns
         * @return this builder
         */
        public Builder excludePackages(Collection<String> packagesToExclude) {
            if (packagesToExclude != null && !packagesToExclude.isEmpty()) {
                this.packagesToExclude.addAll(packagesToExclude);
            }
            return this;
        }

        /**
         * Sets the output directory containing compiled classes to scan (typically target/classes).
         *
         * @param outputDirectory the path to the output directory
         * @return this builder
         */
        public Builder withOutputDirectory(Path outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        /**
         * Runs architecture rule check
         */
        public void check() {
            var rule = new GlobalLoggingArchitectureRule(
                this.allowList,
                this.allowListSuffixes,
                this.packagesToExclude,
                this.outputDirectory
            );
            rule.checkNoSlf4jLoggerFactory();
        }
    }

    /**
     * Checks the rule: no dependency on SLF4J {@code LoggerFactory}, except classes in the allow-list and skipping excluded packages.
     */
    private void checkNoSlf4jLoggerFactory() {
        JavaClasses classes = importClasses();

        // If no class to analyze, then we can skip the rule.
        if (classes.isEmpty()) {
            return;
        }

        ClassesShould classesShould;
        classesShould = noClasses().that().resideOutsideOfPackages(this.packagesToExclude).should();

        classesShould
            .dependOnClassesThat()
            .haveFullyQualifiedName(SLF4J_LOGGER_FACTORY)
            .as("Classes must not depend on SLF4J's LoggerFactory")
            .allowEmptyShould(true)
            .check(classes.that(notInAllowList()));
    }

    private JavaClasses importClasses() {
        if (this.outputDirectory == null) {
            throw new IllegalStateException("Output directory is not configured. Call withOutputDirectory(Path) before running checks.");
        }

        // If the output directory does not exist, we can't import classes'.
        // Using an empty list allows executing the plugin on parent modules, with no code.
        if (!Files.exists(this.outputDirectory)) {
            return new ClassFileImporter().importPaths(emptyList());
        }

        return new ClassFileImporter().importPath(this.outputDirectory);
    }

    private DescribedPredicate<JavaClass> notInAllowList() {
        return new DescribedPredicate<>("not in allow-list") {
            @Override
            public boolean test(JavaClass javaClass) {
                // Check full class name
                if (allowList.contains(javaClass.getName())) {
                    return false;
                }
                // Check simple name suffixes
                String simpleName = javaClass.getSimpleName();
                return allowListSuffixes.stream().noneMatch(simpleName::endsWith);
            }
        };
    }
}
