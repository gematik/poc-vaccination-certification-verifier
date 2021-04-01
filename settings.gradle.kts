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
 * The settings file is used to specify which projects to include in your build.
 *
 * Detailed information about configuring a multi-project build in Gradle can be found
 * in the user manual at https://docs.gradle.org/6.8.2/userguide/multi_project_builds.html
 */

import de.fayard.refreshVersions.bootstrapRefreshVersions

// For more information on how this works, see
// https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_management
pluginManagement { //  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  plugins {
    id("com.github.spotbugs")                 // spotbugs plugin
    id("de.jjohannes.extra-java-module-info") // plugin for non-modular jar-files
  } // end plugins

  resolutionStrategy {
    // intentionally empty
  } // end resolutionStrategy

  // Note 3: Although section "dependencyResolutionManagement" contain these repositories
  //         it is necessary to specify them here as well. Otherwise build fails.
  repositories {
    gradlePluginPortal()
    maven {
      url = uri("https://v2202005121345117714.megasrv.de/maven.repository")
    }
  } // end repositories
} // end pluginManagement section __________________________________________________________________

dependencyResolutionManagement { //  . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  repositories {
    gradlePluginPortal()
    mavenCentral()
    maven {
      url = uri("https://v2202005121345117714.megasrv.de/maven.repository")
    }

    /* Note 4: For proper dependency resolution local repositories are not appropriate. A local
    //         repository is useful only during the development phase.
    //         However, it is possible that packages, "utils" and/or "isoiec8825" and/or crypto
    //         are enhanced together.
    //         Thus during the development phase the enhanced version of "utils" is not (yet)
    //         available in public repositories. For those situations maven-local is appropriate.
    //         Once development is finished, "utils" and/or "isoiec8825" and/or "crypto" are
    //         published mavenLocal() has to be commented out.
    mavenLocal() // */
  } // end repositories
} // end dependencyResolutionManagement ____________________________________________________________

// For setting up refreshVersions plugin see
// https://jmfayard.github.io/refreshVersions/
buildscript { // . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . . .
  // Note 4: Although block "dependencyResolutionManagement" in settings.gradle.kts
  //         covers "gradlePluginPortal" this repositories-block is necessary here.
  //         Otherwise build fails.
  repositories {
    gradlePluginPortal()
  }
  dependencies.classpath("de.fayard.refreshVersions:refreshVersions:0.9.7")
} // end buildScript section _______________________________________________________________________

bootstrapRefreshVersions()

rootProject.name = "vaccination.poc"
include("app")
