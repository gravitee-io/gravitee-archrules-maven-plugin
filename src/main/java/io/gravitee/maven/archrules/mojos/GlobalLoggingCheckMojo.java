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
import io.gravitee.maven.archrules.rules.GlobalLoggingArchitectureRule;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "global-logging-check", defaultPhase = LifecyclePhase.VERIFY)
public class GlobalLoggingCheckMojo extends SkippableMojo {

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

    @Override
    protected void doExecute() throws MojoExecutionException, MojoFailureException {
        getLog().info("Starting Global Logging ArchUnit rules check...");
        logMojoParameters();

        GlobalLoggingArchitectureRule.configure()
            .withOutputDirectory(outputDirectory.toPath())
            .allowIn(allowList)
            .allowInSuffixes(allowListSuffixes)
            .excludePackages(packagesToExclude)
            .check();

        getLog().info("All Global Logging ArchUnit rules passed.");
    }

    private void logMojoParameters() {
        getLog().info("Allow list: " + allowList);

        getLog().info("Allow list suffixes: " + allowListSuffixes);

        getLog().info("Packages to exclude: " + packagesToExclude);
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
}
