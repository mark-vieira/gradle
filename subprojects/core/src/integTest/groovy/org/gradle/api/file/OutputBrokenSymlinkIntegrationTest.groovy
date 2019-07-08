/*
 * Copyright 2019 the original author or authors.
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
package org.gradle.api.file

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import spock.lang.Issue
import spock.lang.Unroll

@Unroll
@Requires(TestPrecondition.SYMLINKS)
class OutputBrokenSymlinkIntegrationTest extends AbstractIntegrationSpec {
    static taskName = 'producesLink'

    @Issue("https://github.com/gradle/gradle/issues/1365")
    def "detect changes to broken symlink in #description"(description, Closure<String> script) {
        def root = file("root").createDir()
        def target = file("target")
        def link = root.file("link")

        buildFile << script(target, link)

        when:
        target.createFile()
        runTask(this)
        then:
        executedAndNotSkipped ":${taskName}"

        when:
        runTask(this)
        then:
        skipped ":${taskName}"

        when:
        target.delete()
        runTask(this)
        then:
        executedAndNotSkipped ":${taskName}"

        when:
        runTask(this)
        then:
        skipped ":${taskName}"

        when:
        target.createFile()
        runTask(this)
        then:
        executedAndNotSkipped ":${taskName}"

        where:
        description                     | script
        'OutputDirectory without cache' | symbolicLinkOutputDirectory
        'OutputFile without cache'      | symbolicLinkOutputFile
    }

    static runTask = { AbstractIntegrationSpec context -> context.run taskName }

    static symbolicLinkOutputDirectory = { TestFile target, TestFile link ->
        """
            import java.nio.file.*
            class ProducesLink extends DefaultTask {
                @OutputDirectory File outputDirectory
    
                @TaskAction execute() {
                    def link = Paths.get('${link}')
                    Files.deleteIfExists(link);
                    Files.createSymbolicLink(link, Paths.get('${target}'));
                }
            }
            
            task ${taskName}(type: ProducesLink) {
                outputDirectory = file '${link.parentFile}'
                outputs.cacheIf { true }
            }
        """
    }

    static symbolicLinkOutputFile = { TestFile target, TestFile link ->
        """
            import java.nio.file.*
            class ProducesLink extends DefaultTask {
                @OutputFile Path outputFile
    
                @TaskAction execute() {
                    Files.deleteIfExists(outputFile);
                    Files.createSymbolicLink(outputFile, Paths.get('${target}'));
                }
            }
            
            task ${taskName}(type: ProducesLink) {
                outputFile = Paths.get('${link}')
                outputs.cacheIf { true }
            }
        """
    }
}
