/*
 * Copyright 2012 the original author or authors.
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





package org.gradle.api.publish.maven.plugins

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import spock.lang.Ignore
import spock.lang.Issue

class MavenPublishMultiProjectIntegTest extends AbstractIntegrationSpec {
    def mavenModule = mavenRepo.module("org.gradle.test", "project1", "1.9")

    def "project dependency correctly reflected in POM if publication coordinates are unchanged"() {
        createBuildScripts("""
project(":project1") {
    dependencies {
        compile project(":project2")
    }
}
        """)

        when:
        run ":project1:publish"

        then:
        def pom = mavenModule.parsedPom
        pom.scopes.runtime.assertDependsOn("org.gradle.test", "project2", "1.9")
    }

    @Issue("GRADLE-443")
    def "project dependency correctly reflected in POM if archivesBaseName is changed"() {
        createBuildScripts("""
project(":project1") {
    dependencies {
        compile project(":project2")
    }
}

project(":project2") {
    archivesBaseName = "changed"
}
        """)

        when:
        run ":project1:publish"

        then:
        def pom = mavenModule.parsedPom
        pom.scopes.runtime.assertDependsOn("org.gradle.test", "changed", "1.9")
    }

    @Issue("GRADLE-443")
    def "project dependency correctly reflected in POM if mavenDeployer.pom.artifactId is changed"() {
        createBuildScripts("""
project(":project1") {
    dependencies {
        compile project(":project2")
    }
}

project(":project2") {
    apply plugin: 'maven'
    uploadArchives {
        repositories {
            mavenDeployer {
                repository(url: "file:///\$rootProject.projectDir/maven-repo")
                pom.artifactId = "changed"
            }
        }
    }
}
        """)

        when:
        run ":project1:publish"

        then:
        def pom = mavenModule.parsedPom
        pom.scopes.runtime.assertDependsOn("org.gradle.test", "changed", "1.9")
    }

    @Ignore("This does not work: fix this as part of making the project coordinates customisable via DSL") // TODO:DAZ
    def "project dependency correctly reflected in POM if dependency publication pom is changed"() {
        createBuildScripts("""
project(":project1") {
    dependencies {
        compile project(":project2")
    }
}

project(":project2") {
    publishing {
        publications {
            maven {
                pom.withXml {
                    asNode().artifactId[0].value = "changed"
                }
            }
        }
    }
}
        """)

        when:
        run ":project1:publish"

        then:
        def pom = mavenModule.parsedPom
        pom.scopes.runtime.assertDependsOn("org.gradle.test", "changed", "1.9")
    }

    def "project dependency correctly reflected in POM if second artifact is published which differs in classifier"() {
        createBuildScripts("""
project(":project1") {
    dependencies {
        compile project(":project2")
    }
}

project(":project2") {
    task jar2(type: Jar) {
        classifier = "other"
    }

    artifacts {
        archives jar2
    }
}
        """)

        when:
        run ":project1:publish"

        then:
        def pom = mavenModule.parsedPom
        pom.scopes.runtime.assertDependsOn("org.gradle.test", "project2", "1.9")
    }

    private void createBuildScripts(String append = "") {
        settingsFile << """
include "project1", "project2"
        """

        buildFile << """
allprojects {
    group = "org.gradle.test"
    version = 1.9
}

subprojects {
    apply plugin: "java"
    apply plugin: "maven-publish"

    publishing {
        repositories {
            maven { url "file:///\$rootProject.projectDir/maven-repo" }
        }
    }
}



$append
        """
    }
}
