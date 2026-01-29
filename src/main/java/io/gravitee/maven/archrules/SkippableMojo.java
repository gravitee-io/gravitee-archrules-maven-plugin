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
package io.gravitee.maven.archrules;

import com.tngtech.archunit.thirdparty.com.google.common.annotations.VisibleForTesting;
import io.gravitee.maven.archrules.exceptions.SkippableRuntimeException;
import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Base Mojo that handles skip, failOnError and common execution logic.
 */
public abstract class SkippableMojo extends AbstractMojo {

    /**
     * Skips the execution of this mojo when set to {@code true}.
     *
     * <p>Can be configured in two ways:</p>
     * <ul>
     *   <li>Command line: {@code mvn ... -Dgravitee.archrules.skip=true}</li>
     *   <li>POM configuration:
     *     <pre>{@code
     * <configuration>
     *   <skip>true</skip>
     * </configuration>
     *     }</pre>
     *   </li>
     * </ul>
     */
    @Parameter(property = "gravitee.archrules.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Determines whether the build should fail if architectural rule violations are detected.
     *
     * <p>Can be configured in two ways:</p>
     * <ul>
     *   <li>Command line: {@code mvn ... -Dgravitee.archrules.failOnError=false}</li>
     *   <li>POM configuration:
     *     <pre>{@code
     * <configuration>
     *   <failOnError>false</failOnError>
     * </configuration>
     *     }</pre>
     *   </li>
     * </ul>
     */
    @Parameter(property = "gravitee.archrules.failOnError", defaultValue = "true")
    protected boolean failOnError;

    @Parameter(defaultValue = "${project.build.outputDirectory}", readonly = true, required = true)
    protected File outputDirectory;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping execution of ArchRulesMojo: " + getClass().getSimpleName());
            return;
        }

        try {
            doExecute();
        } catch (AssertionError e) {
            String message = "ArchUnit violation found:\n" + e.getMessage();
            if (failOnError) {
                throw new MojoFailureException(message, e);
            } else {
                getLog().warn(message);
            }
        } catch (SkippableRuntimeException e) {
            String message = "Error occurred during plugin execution:\n" + e.getMessage();
            if (failOnError) {
                throw new MojoFailureException(message, e);
            } else {
                getLog().warn(message);
            }
        } catch (MojoExecutionException | MojoFailureException e) {
            throw e;
        } catch (Exception e) {
            throw new MojoExecutionException("Error executing " + this.getClass() + " rule", e);
        }
    }

    protected abstract void doExecute() throws MojoExecutionException, MojoFailureException;

    // Setters for tests
    @VisibleForTesting
    public void setSkip(boolean skip) {
        this.skip = skip;
    }

    @VisibleForTesting
    public void setFailOnError(boolean failOnError) {
        this.failOnError = failOnError;
    }

    @VisibleForTesting
    public void setOutputDirectory(File outputDirectory) {
        this.outputDirectory = outputDirectory;
    }
}
