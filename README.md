# Report Portal Java reporter for JBehave tests

> **DISCLAIMER**: We use Google Analytics for sending anonymous usage information such as agent's and client's names, and their versions
> after a successful launch start. This information might help us to improve both ReportPortal backend and client sides. It is used by the
> ReportPortal team only and is not supposed for sharing with 3rd parties.

![CI Build](https://github.com/reportportal/agent-java-jbehave/workflows/CI%20Build/badge.svg?branch=develop)
[ ![Download](https://api.bintray.com/packages/epam/reportportal/agent-java-jbehave/images/download.svg) ](https://bintray.com/epam/reportportal/agent-java-jbehave/_latestVersion)

[![Join Slack chat!](https://reportportal-slack-auto.herokuapp.com/badge.svg)](https://reportportal-slack-auto.herokuapp.com)
[![stackoverflow](https://img.shields.io/badge/reportportal-stackoverflow-orange.svg?style=flat)](http://stackoverflow.com/questions/tagged/reportportal)
[![Build with Love](https://img.shields.io/badge/build%20with-❤%EF%B8%8F%E2%80%8D-lightgrey.svg)](http://reportportal.io?style=flat)

The latest version: 5.0.0-RC-2. Please use `Download` link above to get the agent. Minimal supported JBehave version: 4.8

## Overview: How to Add ReportPortal Logging to Your JBehave Java Project

1. [Configuration](#configuration)
    * Create/update the `reportportal.properties` configuration file
    * Build system configuration
2. Logging configuration
    * [Logback Framework](#logback-framework)
        * Create/update the `logback.xml` file
        * Add Logback dependencies
    * [Log4J Framework](#log4j-framework)
        * Create/update the `log4j2.xml` file
        * Add Log4J dependencies
4. [Running tests](#test-run)
    * via JUnit 4 test runner and Maven plugin

## Configuration

### 'reportportal.properties' configuration file

To start using Report Portal you need to create a file named `reportportal.properties` in your Java project in a source folder
`src/main/resources` or `src/test/resources` (depending on where your tests are located):

**reportportal.properties**

```
rp.endpoint = http://localhost:8080
rp.uuid = e0e541d8-b1cd-426a-ae18-b771173c545a
rp.launch = JBehave Tests
rp.project = default_personal
```

**Property description**

* `rp.endpoint` - the URL for the report portal server (actual link).
* `rp.api.key` - an access token for Report Portal which is used for user identification. It can be found on your report portal user profile
  page.
* `rp.project` - a project ID on which the agent will report test launches. Must be set to one of your assigned projects.
* `rp.launch` - a user-selected identifier of test launches.

### Build system configuration

#### Maven

`pom.xml`

```xml

<project>
    <!-- project declaration omitted -->

    <!-- Add Report Portal repository to get dependencies -->
    <repositories>
        <repository>
            <id>bintray</id>
            <url>http://dl.bintray.com/epam/reportportal</url>
        </repository>
    </repositories>
    
    <properties>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <maven.compiler.target>1.8</maven.compiler.target>
        <maven.compiler.source>1.8</maven.compiler.source>
        <jbehave.core.version>4.8.1</jbehave.core.version> <!-- JBehave binaries version -->
        <embeddables>**/*Stories.java</embeddables> <!-- JBehave story filter -->
        <meta.filter>-skip</meta.filter> <!-- Skip tests tagged with '@skip' tag -->
    </properties>

    <dependencies>
        <dependency>
            <groupId>com.epam.reportportal</groupId>
            <artifactId>agent-java-jbehave</artifactId>
            <version>5.0.0-RC-2</version>
        </dependency>

        <dependency>
            <groupId>org.jbehave</groupId>
            <artifactId>jbehave-core</artifactId>
            <version>${jbehave.core.version}</version>
        </dependency>
    </dependencies>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>3.5.1</version>
                </plugin>
            </plugins>
        </pluginManagement>
        <plugins>
            <plugin>
                <groupId>org.jbehave</groupId>
                <artifactId>jbehave-maven-plugin</artifactId>
                <version>${jbehave.core.version}</version>
                <executions>
                    <execution>
                        <id>embeddable-stories</id>
                        <phase>test</phase>
                        <configuration>
                            <includes>
                                <include>${embeddables}</include>
                            </includes>
                            <excludes/>
                            <ignoreFailureInStories>true</ignoreFailureInStories>
                            <ignoreFailureInView>false</ignoreFailureInView>
                            <threads>1</threads>
                            <metaFilters>
                                <metaFilter>${meta.filter}</metaFilter>
                            </metaFilters>
                        </configuration>
                        <goals>
                            <goal>run-stories-as-embeddables</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
```

#### Gradle
TBD
```groovy

plugins {
   id 'java'
}

sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8

repositories {
   mavenCentral()
   maven { url "http://dl.bintray.com/epam/reportportal" }
}

dependencies {
   testCompile 'org.jbehave:jbehave-core:4.8.1'
   testCompile 'com.epam.reportportal:agent-java-jbehave:5.0.0-RC-2'
}

test {
   outputs.upToDateWhen { return false }
}


```