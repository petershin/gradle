/*
 * Copyright 2018 the original author or authors.
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

package org.gradle.groovy.scripts

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.BuildOperationsFixture
import org.gradle.internal.scripts.CompileScriptBuildOperationType
import org.gradle.internal.scripts.ScriptCompileStage

class GroovyCompileScriptBuildOperationIntegrationTest extends AbstractIntegrationSpec {

    def operations = new BuildOperationsFixture(executer, temporaryFolder)

    def setup() {
        executer.requireOwnGradleUserHomeDir()
    }

    def "captures script compilation build operations"() {
        given:
        settingsFile << "println 'settings.gradle'"
        buildFile << """
            apply from: 'script.gradle'
            println 'build.gradle'
        """

        file("script.gradle") << "println 'script.gradle'"
        file("init.gradle") << "println 'init.gradle'"

        when:
        succeeds 'help', '-I', 'init.gradle'

        then:
        def scriptCompiles = operations.all(CompileScriptBuildOperationType)

        scriptCompiles*.displayName == [
            'Compile script init.gradle (CLASSPATH)',
            'Compile script init.gradle (BODY)',
            'Compile script settings.gradle (CLASSPATH)',
            'Compile script settings.gradle (BODY)',
            'Compile script build.gradle (CLASSPATH)',
            'Compile script build.gradle (BODY)',
            'Compile script script.gradle (CLASSPATH)',
            'Compile script script.gradle (BODY)'
        ]

        scriptCompiles.details*.language == [
            'groovy',
            'groovy',
            'groovy',
            'groovy',
            'groovy',
            'groovy',
            'groovy',
            'groovy'
        ]
        scriptCompiles.details*.stage == [
            ScriptCompileStage.CLASSPATH.name(),
            ScriptCompileStage.BODY.name(),
            ScriptCompileStage.CLASSPATH.name(),
            ScriptCompileStage.BODY.name(),
            ScriptCompileStage.CLASSPATH.name(),
            ScriptCompileStage.BODY.name(),
            ScriptCompileStage.CLASSPATH.name(),
            ScriptCompileStage.BODY.name(),
        ]

        when: // already compiled build scripts
        succeeds "help", '-I', 'init.gradle'

        then:
        operations.all(CompileScriptBuildOperationType).size() == 0

        when: // changing cached build script
        buildFile << "println 'appending build logic'"
        succeeds "help", '-I', 'init.gradle'
        scriptCompiles = operations.all(CompileScriptBuildOperationType)

        then: // affected build script is recompiled
        scriptCompiles.size() == 2
        scriptCompiles*.displayName == [
            'Compile script build.gradle (CLASSPATH)',
            'Compile script build.gradle (BODY)'
        ]

    }

    def "captures shared scripts"() {
        given:
        file("shared.gradle") << "println 'shared.gradle'"

        buildFile << """
            apply from: 'shared.gradle'
            println 'build.gradle'
        """


        when:
        succeeds "help"

        then:
        def scriptCompiles = operations.all(CompileScriptBuildOperationType)

        scriptCompiles*.displayName == [
            'Compile script build.gradle (CLASSPATH)',
            'Compile script build.gradle (BODY)',
            'Compile script shared.gradle (CLASSPATH)',
            'Compile script shared.gradle (BODY)'
        ]

        scriptCompiles.details*.language == [
            'groovy',
            'groovy',
            'groovy',
            'groovy'
        ]
        scriptCompiles.details*.stage == [
            ScriptCompileStage.CLASSPATH.name(),
            ScriptCompileStage.BODY.name(),
            ScriptCompileStage.CLASSPATH.name(),
            ScriptCompileStage.BODY.name()
        ]

        when: // already compiled build scripts
        file('otherBuild/build.gradle') << "apply from: '../shared.gradle'"

        executer.usingProjectDirectory(file('otherBuild'))
        succeeds 'help'
        scriptCompiles = operations.all(CompileScriptBuildOperationType)

        then:
        scriptCompiles.details*.language == [
            'groovy',
            'groovy'
        ]

        scriptCompiles.details*.stage == [
            ScriptCompileStage.CLASSPATH.name(),
            ScriptCompileStage.BODY.name()
        ]

        scriptCompiles*.displayName == [
            'Compile script build.gradle (CLASSPATH)',
            'Compile script build.gradle (BODY)',
        ]
    }
}
