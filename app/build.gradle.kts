/*
 * Copyright (c) 2021 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * This generated file contains a sample Java application project to get you started.
 * For more details take a look at the 'Building Java & JVM projects' chapter in the Gradle
 * User Manual available at https://docs.gradle.org/6.8.2/userguide/building_java_projects.html
 */

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.util.Properties

// --- estimate versions from "versions.properties"
// Note: File "versions.properties" is updated a gradle plugin using task "refreshVersions"
var verCheckstyle = ""
var verCborCrack = ""
var verJacoco = ""
var verPmd = ""
try {
    Files.newInputStream(Paths.get("${rootProject.projectDir}/versions.properties")).use { fis ->
        val prop = Properties()
        prop.load(fis)
        verCheckstyle = prop.getProperty("version.com.puppycrawl.tools..checkstyle", "unknown")
        verCborCrack = prop.getProperty("version.co.nstant.in..cbor", "unknown")
        verJacoco = prop.getProperty("version.org.jacoco..org.jacoco.agent")
        verPmd = prop.getProperty("version.net.sourceforge.pmd..pmd-java", "unknown")
    }
} catch (e: IOException) {
    e.printStackTrace()
} // end catch(IOException)
// ... versions known

// section for loading plugins . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
plugins {
    // Apply the application plugin to add support for building a CLI application in Java.
    id("application")
    id("eclipse")
    id("checkstyle")
    id("com.github.spotbugs")
    id("de.jjohannes.extra-java-module-info") // plugin for non-modular jar-files
    id("jacoco")
    id("java-library")
    id("pmd")
} // end plugins ___________________________________________________________________________________

/*
// Note 1: This block is necessary for task "refreshVersions".
// Note 2: This block is not necessary for building the project by gradle.
// Deprecated on 2021-01-20 with gradle version 6.8.
// Instead block "dependencyResolutionManagement" in settings.gradle.kts covers this information.
repositories { //  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  gradlePluginPortal()
  mavenCentral()
  maven {
    url = uri("https://v2202005121345117714.megasrv.de/maven.repository")
  }
  mavenLocal()
} // end repositories ___________________________________________________________________________ */

// set JavaVersion . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11

    modularity.inferModulePath.set(true) // since Gradle 6.4
} // end JavaVersion _______________________________________________________________________________

// Extra Information for non-modular jar-files.
extraJavaModuleInfo { // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // Add module information for all direct and transitive dependencies that are not modules, see
    // https://github.com/jjohannes/extra-java-module-info

    // Note 1: If the following line-of-code sets to FALSE, then
    //         there is no need to list transitive non-modular-jars.
    // Note 2: If the following line-of-code sets to TRUE, then
    //         all non-modular jars have to be listed in this section
    failOnMissingModuleInfo.set(false)

    // CBOR support from c-rack
    module("cbor-" + verCborCrack + ".jar", "co.nstant.in.cbor", verCborCrack) {
        exports("co.nstant.in.cbor")
        exports("co.nstant.in.cbor.builder")
        exports("co.nstant.in.cbor.decoder")
        exports("co.nstant.in.cbor.encoder")
        exports("co.nstant.in.cbor.model")
    } // */
} // end extraJavaModuleInfo _______________________________________________________________________

gradle.taskGraph.whenReady { //  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
    // set flag indicating whether a release is build
    val flagRelease = hasTask(":release") || hasTask(":" + project.name + ":release")

    // begin checkstyle configuration
    checkstyle {
        configFile = file(
                "${rootProject.projectDir}/config/afi_google_checks_" + verCheckstyle + ".xml"
        )
        isIgnoreFailures = !flagRelease // don't ignore errors and warnings for releases
    }

    tasks.withType<Checkstyle>().configureEach {
        reports {
            xml.isEnabled = false
            html.isEnabled = true
        }
    }
    // end checkstyle configuration

    // begin jacoco configuration
    jacoco {
        toolVersion = verJacoco
    } // end jacoco

    tasks {
        jacocoTestCoverageVerification {
            val jacocoLimit = if (flagRelease) "1.0" else "0.1"
            // For defining rules see e.g.: https://reflectoring.io/jacoco/
            violationRules {
                rule {
                    limit {
                        counter = "INSTRUCTION"
                        value   = "COVEREDRATIO"
                        minimum = jacocoLimit.toBigDecimal()
                    }
                }
                rule {
                    limit {
                        counter = "BRANCH"
                        value   = "COVEREDRATIO"
                        minimum = jacocoLimit.toBigDecimal()
                    }
                }
            }
        }
    } // end tasks
    // end   jacoco configuration

    // begin pmd configuration
    pmd {
        toolVersion = verPmd
        ruleSets = mutableListOf<String>()
        ruleSetFiles = files(
                "${rootProject.projectDir}/config/afi_quickstart_" + verPmd + ".xml"
        )
        isIgnoreFailures = !flagRelease // don't ignore errors and warnings for releases
    }

    tasks.withType<Pmd>().configureEach {
        reports {
            xml.isEnabled = false
            html.isEnabled = true
        }
    }
    // end pmd configuration

    // begin spotbugs configuration
    spotbugs {
        setEffort("max")      // min, less, more, max
        setReportLevel("low") // high, medium, low
        ignoreFailures.set(!flagRelease)
    } // end spotbugs configuration
    // end spotbugs configuration
} // end gradle.taskGraph.whenReady ________________________________________________________________

// configure dependencies  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
dependencies {
    // dependencies for plugins
    checkstyle("com.puppycrawl.tools:checkstyle:_")
    jacocoAnt("org.jacoco:org.jacoco.ant:_")
    jacocoAgent("org.jacoco:org.jacoco.agent:_")
    pmd("net.sourceforge.pmd:pmd-java:_")

    // null-annotations, e.g. in package-info.java
    api("com.github.spotbugs:spotbugs-annotations:_")

    // logging instead of System.print
    implementation("org.slf4j:slf4j-api:_")
    runtimeOnly("org.slf4j:slf4j-simple:_")

    implementation("com.gmail.alfred65fiedler:com.gmail.alfred65fiedler.crypto:_") // cryptography
    implementation("com.gmail.alfred65fiedler:com.gmail.alfred65fiedler.tlv:_") // TLV object
    implementation("com.gmail.alfred65fiedler:com.gmail.alfred65fiedler.utils:_") // utility functions

    // Use JUnit Jupiter API for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:_")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")

    // CBOR-support from cbor
    implementation("co.nstant.in:cbor:_")

    // QR-code
    implementation("com.google.zxing:core:_")
    implementation("com.google.zxing:javase:_")
}

// begin application definition  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
application {
    mainModule.set("de.gematik.poc.vaccination")
    mainClass.set("de.gematik.poc.vaccination.userinterface.CmdLine")
} // end application _______________________________________________________________________________


// begin task  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
tasks {
    // force Gradle to use an appropriate compile option such that source-files are treated UTF-8
    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    jacocoTestCoverageVerification {
        // This task probably fails during software development because of low coverage.
        // It seems useful that all other checks are performed before the build fails.
        shouldRunAfter(
                "checkstyleMain", "checkstyleTest",
                "pmdMain",        "pmdTest",
                "spotbugsMain",   "spotbugsTest"
        )
    } // end jacocoTestCoverageVerification

    check{dependsOn("jacocoTestCoverageVerification")}
} // end   task ____________________________________________________________________________________

// section configuring test tasks  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
tasks.test {
    useJUnitPlatform()

    testLogging {
        events("PASSED", "FAILED", "SKIPPED")
    }

    // ensures that a report is generated immediately after test-task completes
    finalizedBy("jacocoTestReport")
} // end test tasks ________________________________________________________________________________
