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

import com.tngtech.archunit.thirdparty.com.google.common.annotations.VisibleForTesting;
import io.gravitee.maven.archrules.SkippableMojo;
import io.gravitee.maven.archrules.rules.ExecutionContextLoggingArchitectureRule;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "execution-context-logging-check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class ExecutionContextLoggingCheckMojo extends SkippableMojo {

    /**
     * List of fully qualified class names to exclude from rule checks.
     *
     * <p>Can be configured in two ways:</p>
     * <ul>
     *   <li>Command line: {@code mvn ... -Dgravitee.archrules.allowList=com.example.LegacyClass,com.example.AnotherClass}</li>
     *   <li>POM configuration:
     *     <pre>{@code
     * <configuration>
     *   <allowList>
     *     <allowList>com.example.LegacyClass</allowList>
     *     <allowList>com.example.AnotherClass</allowList>
     *   </allowList>
     * </configuration>
     *     }</pre>
     *   </li>
     * </ul>
     */
    @Parameter(property = "gravitee.archrules.allowList")
    private List<String> allowList;

    /**
     * List of class name suffixes to exclude from rule checks (e.g., "ConfigurationEvaluator").
     *
     * <p>Can be configured in two ways:</p>
     * <ul>
     *   <li>Command line: {@code mvn ... -Dgravitee.archrules.allowListSuffixes=Test,TestHelper}</li>
     *   <li>POM configuration:
     *     <pre>{@code
     * <configuration>
     *   <allowListSuffixes>
     *     <allowListSuffix>Test</allowListSuffix>
     *     <allowListSuffix>TestHelper</allowListSuffix>
     *   </allowListSuffixes>
     * </configuration>
     *     }</pre>
     *   </li>
     * </ul>
     */
    @Parameter(property = "gravitee.archrules.allowListSuffixes")
    private List<String> allowListSuffixes;

    /**
     * List of packages to exclude from scanning.
     *
     * <p>Can be configured in two ways:</p>
     * <ul>
     *   <li>Command line: {@code mvn ... -Dgravitee.archrules.packagesToExclude=com.example.legacy..,com.example.generated..}</li>
     *   <li>POM configuration:
     *     <pre>{@code
     * <configuration>
     *   <packagesToExclude>
     *     <packageToExclude>com.example.legacy..</packageToExclude>
     *     <packageToExclude>com.example.generated..</packageToExclude>
     *   </packagesToExclude>
     * </configuration>
     *     }</pre>
     *   </li>
     * </ul>
     */
    @Parameter(property = "gravitee.archrules.packagesToExclude")
    private List<String> packagesToExclude;

    /**
     * List of fully qualified class names to exclude from execution context rule checks.
     * <p>
     * This parameter allows specifying specific classes that should be ignored during the
     * evaluation of execution context-related architectural rules. By providing the class
     * names in this list, they will be excluded from the scanning and verification process.
     * <p>
     * Typical usage includes excluding known classes that are irrelevant to the rule checks
     * or cases where the rules should not apply to certain classes.
     *
     * <p>Can be configured in two ways:</p>
     * <ul>
     *   <li>Command line: {@code mvn ... -Dgravitee.archrules.ignoreExecutionContextClasses=com.example.FakeContext,com.example.MockContext}</li>
     *   <li>POM configuration:
     *     <pre>{@code
     * <configuration>
     *   <ignoreExecutionContextClasses>
     *     <ignoreExecutionContextClass>com.example.FakeContext</ignoreExecutionContextClass>
     *     <ignoreExecutionContextClass>com.example.MockContext</ignoreExecutionContextClass>
     *   </ignoreExecutionContextClasses>
     * </configuration>
     *     }</pre>
     *   </li>
     * </ul>
     */
    @Parameter(property = "gravitee.archrules.ignoreExecutionContextClasses")
    private List<String> ignoreExecutionContextClasses;

    /**
     * List of additional context class names to be scanned for logging violations.
     * <p>
     * This parameter allows specifying custom context types that should be treated as execution contexts,
     * even if they don't contain "ExecutionContext" in their name. This is useful for scanning specialized
     * context types like KafkaConnectionContext or custom context implementations.
     * <p>
     * When a method has a parameter of one of these types, the rule will enforce that logging should be
     * done through {@code ctx.withLogger(log)} instead of calling the logger directly.
     *
     * <p>Can be configured in two ways:</p>
     * <ul>
     *   <li>Command line: {@code mvn ... -Dgravitee.archrules.additionalContextClasses=io.gravitee.node.api.kafka.KafkaConnectionContext}</li>
     *   <li>POM configuration:
     *     <pre>{@code
     * <configuration>
     *   <additionalContextClasses>
     *     <additionalContextClass>io.gravitee.node.api.kafka.KafkaConnectionContext</additionalContextClass>
     *     <additionalContextClass>com.example.CustomContext</additionalContextClass>
     *   </additionalContextClasses>
     * </configuration>
     *     }</pre>
     *   </li>
     * </ul>
     */
    @Parameter(property = "gravitee.archrules.additionalContextClasses")
    private List<String> additionalContextClasses;

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting Execution Context Logging ArchUnit rules check...");
        logMojoParameters();

        ExecutionContextLoggingArchitectureRule.configure()
            .withOutputDirectory(outputDirectory.toPath())
            .withWarningHandler(msg -> getLog().warn(msg))
            .allowIn(allowList)
            .allowInSuffixes(allowListSuffixes)
            .excludePackages(packagesToExclude)
            .ignoreExecutionContextClass(ignoreExecutionContextClasses)
            .withAdditionalContextClasses(additionalContextClasses)
            .check();

        getLog().info("All Execution Context Logging ArchUnit rules passed.");
    }

    private void logMojoParameters() {
        getLog().info("Allow list: " + allowList);

        getLog().info("Allow list suffixes: " + allowListSuffixes);

        getLog().info("Packages to exclude: " + packagesToExclude);

        getLog().info("Ignore ExecutionContext classes: " + ignoreExecutionContextClasses);

        getLog().info("Additional context classes: " + additionalContextClasses);
    }

    @VisibleForTesting
    public void setAllowList(List<String> allowList) {
        this.allowList = allowList;
    }

    @VisibleForTesting
    public void setAllowListSuffixes(List<String> allowListSuffixes) {
        this.allowListSuffixes = allowListSuffixes;
    }

    @VisibleForTesting
    public void setPackagesToExclude(List<String> packagesToExclude) {
        this.packagesToExclude = packagesToExclude;
    }

    @VisibleForTesting
    public void setIgnoreExecutionContextClasses(List<String> ignoreExecutionContextClasses) {
        this.ignoreExecutionContextClasses = ignoreExecutionContextClasses;
    }

    @VisibleForTesting
    public void setAdditionalContextClasses(List<String> additionalContextClasses) {
        this.additionalContextClasses = additionalContextClasses;
    }
}
