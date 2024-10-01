# Eclipse External null Annotations (EEA)

[![Build Status](https://github.com/lastnpe/eclipse-null-eea-augments/workflows/Build/badge.svg "GitHub Actions")](https://github.com/lastnpe/eclipse-null-eea-augments/actions?query=workflow%3A%22Build%22)
[![License](https://img.shields.io/github/license/lastnpe/eclipse-null-eea-augments.svg?color=blue)](LICENSE.txt)
[![Contributor Covenant](https://img.shields.io/badge/Contributor%20Covenant-v2.1%20adopted-ff69b4.svg)](CODE_OF_CONDUCT.md)
[![Maven Central](https://img.shields.io/maven-central/v/org.lastnpe.eea/eea-all)](https://central.sonatype.com/artifact/org.lastnpe.eea/eea-all)


1. [What is this?](#what-is-this)
1. [How to use this](#usage)
   1. [Binaries](#binaries)
   1. [Building from Sources](#building)
   1. [Validating/Updating EEA files](#validate_update)
1. [How to contribute](#contribute)
1. [Troubleshooting](#troubleshooting)
1. [Future](#future)
1. [License](#license)


## <a name="what-is-this"></a>What is this?

This repository contains *.eea files and example projects how to use this.

If you like/use this project, a Star / Watch / Follow on GitHub is appreciated.


## <a name="usage"></a>How to use this

General usage of External Null Annotations in Eclipse is documented at
https://help.eclipse.org/latest/index.jsp?topic=/org.eclipse.jdt.doc.user/tasks/task-using_external_null_annotations.htm

[See theses slides here](https://www.slideshare.net/mikervorburger/the-end-of-the-world-as-we-know-it-aka-your-last-nullpointerexception-1b-bugs) from this
[EclipseCon Europe 2016 presentation](https://www.eclipsecon.org/europe2016/session/end-world-we-know-it-aka-your-last-nullpointerexception-1b-bugs) for some background about this project.

To automatically Enable Annotation-based Null Analysis in the Eclipse Project Preferences correctly (e.g. when you import the `examples/` here), we highly
recommend you install the [eclipse-external-annotations-m2e-plugin](https://github.com/lastnpe/eclipse-external-annotations-m2e-plugin) Maven IDE (M2E) Configurator Eclipse plugin.
(On Eclipse m2e versions < 1.8 (shipped with Oxygen), you also had to install [m2e-jdt-compiler](https://github.com/jbosstools/m2e-jdt-compiler), but with M2E 1.8 in Oxygen that is not necessary anymore, even harmful; see below.)


### <a id="binaries"></a>Binaries

Latest **Release** binaries are available on Maven central, see https://search.maven.org/search?q=g%3Aorg.lastnpe.eea

Latest **Snapshot** binaries are available via the [mvn-snapshots-repo](https://github.com/lastnpe/eclipse-null-eea-augments/tree/mvn-snapshots-repo) git branch.
You need to add this repository configuration to your Maven `settings.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<settings xmlns="http://maven.apache.org/SETTINGS/1.2.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.2.0 https://maven.apache.org/xsd/settings-1.2.0.xsd">
  <profiles>
    <profile>
      <repositories>
        <repository>
          <id>lastnpe-snapshots</id>
          <name>lastnpe-snapshots</name>
          <url>https://raw.githubusercontent.com/lastnpe/eclipse-null-eea-augments/mvn-snapshots-repo</url>
          <releases><enabled>false</enabled></releases>
          <snapshots><enabled>true</enabled></snapshots>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>lastnpe-snapshots</activeProfile>
  </activeProfiles>
</settings>
```


### <a id="building"></a>Building from Sources

The project also uses the [maven-toolchains-plugin](https://maven.apache.org/plugins/maven-toolchains-plugin/) which decouples the JDK that is
used to execute Maven and it's plug-ins from the target JDK that is used for compilation and/or unit testing. This ensures full binary
compatibility of the compiled artifacts with the runtime library of the required target JDK.

To build the project follow these steps:

1. Download and install Java 17 **AND** Java 21 SDKs, e.g. from:
   - Java 17: https://adoptium.net/releases.html?variant=openjdk17 or https://www.azul.com/downloads/?version=java-17-lts&package=jdk#download-openjdk
   - Java 21: https://adoptium.net/releases.html?variant=openjdk21 or https://www.azul.com/downloads/?version=java-21-lts&package=jdk#download-openjdk

1. Download and install the latest [Maven distribution](https://maven.apache.org/download.cgi).

1. In your user home directory create the file `.m2/toolchains.xml` with the following content:

   ```xml
   <?xml version="1.0" encoding="UTF8"?>
   <toolchains xmlns="http://maven.apache.org/TOOLCHAINS/1.1.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://maven.apache.org/TOOLCHAINS/1.1.0 https://maven.apache.org/xsd/toolchains-1.1.0.xsd">
      <toolchain>
         <type>jdk</type>
         <provides>
            <version>17</version>
            <vendor>default</vendor>
         </provides>
         <configuration>
            <jdkHome>[PATH_TO_YOUR_JDK_17]</jdkHome>
         </configuration>
      </toolchain>
      <toolchain>
         <type>jdk</type>
         <provides>
            <version>21</version>
            <vendor>default</vendor>
         </provides>
         <configuration>
            <jdkHome>[PATH_TO_YOUR_JDK_21]</jdkHome>
         </configuration>
      </toolchain>
   </toolchains>
   ```

   Set the `[PATH_TO_YOUR_JDK_17]`/`[PATH_TO_YOUR_JDK_21]` parameters accordingly
   to where you installed the JDKs.

1. Checkout the code, e.g. using:

    - `git clone https://github.com/lastnpe/eclipse-null-eea-augments`

1. Run `mvn clean verify` in the project root directory. This will execute compilation, unit-testing, integration-testing and
   packaging of all artifacts.


### <a name="validate_update"></a>Validating/Updating EEA files

The EEA files can be validated/updated using:

```bash
# validate all EEA files of all eea-* modules
mvn compile

# validate all EEA files of a specific module
mvn compile -am -pl <MODULE_NAME>
mvn compile -am -pl libraries/gson

# update/regenerate all EEA files of all eea-* modules
mvn compile -Deea-generator.action=generate

# update/regenerate all EEA files of a specific module
mvn compile -Deea-generator.action=generate -am -pl <MODULE_NAME>
mvn compile -Deea-generator.action=generate -am -pl libraries/gson
```

Updating EEA files will:
- add new types/fields/methods found
- remove obsolete declarations from the EEA files
- preserve null/non-null annotations specified for existing fields/methods


## <a name="contribute"></a>How to contribute

Please consult the [CONTRIBUTING.md](CONTRIBUTING.md).

This project aims to develop an active community of contributors, and not remain controlled by a single person.
Anyone making 3 intelligent contributions to this repo may ask to be promoted from a contributor to a committer with full write access by opening an issue requesting it.
(We reserve the right to remove committers in exceptional circumstances, and after long periods of inactivity.)

We intend to liberally and quickly merge any contributions with additions to EEA, and avoid delays due to lengthy reviews
(which are anyway [kind of difficult to do rapidly and at scale until we solve issue #16](https://github.com/lastnpe/eclipse-null-eea-augments/issues/16)),
based on the idea that having an EEA that can be tested is better than none.

We intend to spend more time and request community feedback from engaged previous contributors on any proposed changes to existing EEA in this repo.
When making contributions with changes, please explain what is wrong in the current version in the commit message.

We generally do not "self merge", but let other committers merge our own changes.


## <a name="troubleshooting"></a>Troubleshooting

* Uninstall the [jbosstools/m2e-jdt-compiler](https://github.com/jbosstools/m2e-jdt-compiler) to fix this problem: _Conflicting lifecycle mapping (plugin execution "org.apache.maven.plugins:maven-compiler-plugin:3.5.1:compile (execution: default-compile, phase: compile)"). To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration._

* Do `mvn install` of the `examples/maven/jdt-ecj-settings` to fix this problem, due to [M2E Bug 522393](https://bugs.eclipse.org/bugs/show_bug.cgi?id=522393): _CoreException: Could not get the value for parameter compilerId for plugin execution default-testCompile: PluginResolutionException: Plugin org.apache.maven.plugins:maven-compiler-plugin:3.5.1 or one of its dependencies could not be resolved: Failure to find ch.vorburger.nulls.examples:jdt-ecj-settings:jar:1.0.0-SNAPSHOT_


## <a name="future"></a>Future

If this is found to be of general interest, perhaps this could move to eclipse.org. If this happens, it would be imperative to keep it very easy for anyone to contribute via Pull Requests on GitHub (and not, or not you only, eclipse.org Gerrit changes).


## <a name="license"></a>License

All files are released under the [Eclipse Public License 2.0](LICENSE.txt).

Individual files contain the following tag instead of the full license text:
```
SPDX-License-Identifier: EPL-2.0
```

This enables machine processing of license information based on the SPDX License Identifiers that are available here: https://spdx.org/licenses/.
