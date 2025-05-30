/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.plugins;

import org.gradle.api.Incubating;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.plugins.jvm.JvmTestSuite;
import org.gradle.api.plugins.jvm.internal.DefaultJvmTestSuite;
import org.gradle.api.tasks.testing.Test;
import org.gradle.testing.base.TestingExtension;
import org.gradle.testing.base.plugins.TestSuiteBasePlugin;

import java.util.concurrent.Callable;

/**
 * A {@link org.gradle.api.Plugin} that adds extensions for declaring, compiling and running {@link JvmTestSuite}s.
 * <p>
 * This plugin provides conventions for several things:
 * <ul>
 *     <li>All other {@code JvmTestSuite} will use the JUnit Jupiter testing framework unless specified otherwise.</li>
 *     <li>A single test suite target is added to each {@code JvmTestSuite}.</li>
 *
 * </ul>
 *
 * @since 7.3
 * @see <a href="https://docs.gradle.org/current/userguide/jvm_test_suite_plugin.html">Test Suite plugin reference</a>
 */
@Incubating
public abstract class JvmTestSuitePlugin implements Plugin<Project> {
    public static final String DEFAULT_TEST_SUITE_NAME = "test";

    @Override
    public void apply(Project project) {
        project.getPluginManager().apply(TestSuiteBasePlugin.class);
        project.getPluginManager().apply(JavaBasePlugin.class);

        JavaPluginExtension java = project.getExtensions().getByType(JavaPluginExtension.class);
        project.getTasks().withType(Test.class).configureEach(test -> {
            test.getModularity().getInferModulePath().convention(java.getModularity().getInferModulePath());
        });

        TestingExtension testing = project.getExtensions().getByType(TestingExtension.class);
        testing.getSuites().registerBinding(JvmTestSuite.class, DefaultJvmTestSuite.class);

        testing.getSuites().withType(JvmTestSuite.class).all(testSuite -> {
            testSuite.getTargets().all(target -> {
                target.getTestTask().configure(test -> {
                    ((ConfigurableFileCollection)test.getTestClassesDirs()).convention((Callable<FileCollection>) () ->
                        testSuite.getSources().getOutput().getClassesDirs()
                    );
                    ((ConfigurableFileCollection)test.getClasspath()).convention((Callable<FileCollection>) () ->
                        testSuite.getSources().getRuntimeClasspath()
                    );
                });
                target.getBinaryResultsDirectory().convention(target.getTestTask().flatMap(Test::getBinaryResultsDirectory));
            });
        });
    }

}
