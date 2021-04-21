/*
 * Copyright 2016 the original author or authors.
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

package org.gradle.launcher

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.integtests.fixtures.AvailableJavaHomes
import org.gradle.integtests.fixtures.executer.GradleContextualExecuter
import org.gradle.internal.jvm.Jvm
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.GradleVersion
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import spock.lang.IgnoreIf
import spock.lang.Issue


class SupportedBuildJvmIntegrationTest extends AbstractIntegrationSpec {

    @Requires(TestPrecondition.SYMLINKS)
    def "can start Gradle with a JDK that contains symlinks"() {
        // Zulu sets their Java distribution up like this
        def installedJdk = Jvm.current().javaHome
        def symlinkedJdk = file("symlink-jdk")
        installedJdk.listFiles().each {
            symlinkedJdk.file(it.name).createLink(it)
        }
        file("gradle.properties").writeProperties("org.gradle.java.home": symlinkedJdk.canonicalPath)
        expect:
        succeeds("help")
    }

    @Issue("https://github.com/gradle/gradle/issues/16816")
    @Requires(TestPrecondition.SYMLINKS)
    def "can successful start after a running daemon's JDK has been removed"() {
        // Zulu sets their Java distribution up like this
        def installedJdk = Jvm.current().javaHome
        def removedJdk = file("removed-jdk")
        removedJdk.mkdir()
        new TestFile(installedJdk).copyTo(removedJdk)

        // start one JVM with the removed-jdk
        file("gradle.properties").writeProperties("org.gradle.java.home": removedJdk.canonicalPath)
        succeeds("help")

        when:
        // remove the other JDK
        removedJdk.deleteDir()
        // try to start with the other-jdk
        file("gradle.properties").writeProperties("org.gradle.java.home": installedJdk.canonicalPath)
        then:
        succeeds("help")

    }

    @IgnoreIf({ GradleContextualExecuter.embedded }) // This test requires to start Gradle from scratch with the wrong Java version
    @Requires(adhoc = { AvailableJavaHomes.getJdks("1.6", "1.7") })
    def "provides reasonable failure message when attempting to run under java #jdk.javaVersion"() {
        given:
        executer.withJavaHome(jdk.javaHome)

        expect:
        fails("help")
        failure.assertHasErrorOutput("Gradle ${GradleVersion.current().version} requires Java 8 or later to run. You are currently using Java ${jdk.javaVersion.majorVersion}.")

        where:
        jdk << AvailableJavaHomes.getJdks("1.6", "1.7")
    }

    @Requires(adhoc = { AvailableJavaHomes.getJdks("1.6", "1.7") })
    def "fails when build is configured to use Java #jdk.javaVersion"() {
        given:
        file("gradle.properties").writeProperties("org.gradle.java.home": jdk.javaHome.canonicalPath)

        expect:
        fails("help")
        failure.assertHasDescription("Gradle ${GradleVersion.current().version} requires Java 8 or later to run. Your build is currently configured to use Java ${jdk.javaVersion.majorVersion}.")

        where:
        jdk << AvailableJavaHomes.getJdks("1.6", "1.7")
    }
}
