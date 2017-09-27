[![Build Status](https://travis-ci.org/lastnpe/eclipse-null-eea-augments.svg)](https://travis-ci.org/lastnpe/eclipse-null-eea-augments)

# Eclipse External null Annotations (EEA)

This repository contains *.eea files and example projects how to use this.

If you like/use this project, a Star / Watch / Follow on GitHub is appreciated.


## How to use this

[See theses slides here](http://www.slideshare.net/mikervorburger/the-end-of-the-world-as-we-know-it-aka-your-last-nullpointerexception-1b-bugs) from this [EclipseCon Europe 2016 presentation](https://www.eclipsecon.org/europe2016/session/end-world-we-know-it-aka-your-last-nullpointerexception-1b-bugs) for some background about this project.

To automatically Enable Annotation-based Null Analysis in the Eclipse Project Preferences correctly (e.g. when you import the the `examples/` here), we highly recommend you install the [eclipse-external-annotations-m2e-plugin](https://github.com/lastnpe/eclipse-external-annotations-m2e-plugin) Maven IDE (M2E) Configurator Eclipse plugin.  (On Eclipse m2e versions < 1.8 (shipped with Oxygen), you also had to install [m2e-jdt-compiler](https://github.com/jbosstools/m2e-jdt-compiler), but with M2E 1.8 in Oxygen that is not necessary anymore, even harmful; see below.)


## Contribute

This project aims to develop an active community of contributors, and not remain controlled by a single person.  Anyone making 3 intelligent contributions to this repo may ask to be promoted from a contributor to a committer with full write access by opening an issue requesting it.  (We reserve the right to re-remove committers in exceptional circumstances, and after long periods of inactivity.)

We intend to liberally and quickly merge any contributions with additions to EEA, and avoid delays due to lengthy reviews 
(which are anyway [kind of difficult to do rapidly and at scale until we solve issue #16](https://github.com/lastnpe/eclipse-null-eea-augments/issues/16)), 
based on the idea that having an EEA that can be tested is better than none.

We intend to spend more time and request community feedback from engaged previous contributors on any proposed changes to existing EEA in this repo.  When making contributions with changes, please explain what is wrong in the current version in the commit message.

We generally do not "self merge", but let other committers merge our own changes.

We hang out and chat about this project on https://mattermost.eclipse.org/eclipse/channels/jdt-null-analysis


## Troubleshooting

* Uninstall the [jbosstools/m2e-jdt-compiler](https://github.com/jbosstools/m2e-jdt-compiler) to fix this problem: _Conflicting lifecycle mapping (plugin execution "org.apache.maven.plugins:maven-compiler-plugin:3.5.1:compile (execution: default-compile, phase: compile)"). To enable full functionality, remove the conflicting mapping and run Maven->Update Project Configuration._

* Do `mvn install` of the `examples/maven/jdt-ecj-settings` to fix this problem, due to [M2E Bug 522393](https://bugs.eclipse.org/bugs/show_bug.cgi?id=522393): _CoreException: Could not get the value for parameter compilerId for plugin execution default-testCompile: PluginResolutionException: Plugin org.apache.maven.plugins:maven-compiler-plugin:3.5.1 or one of its dependencies could not be resolved: Failure to find ch.vorburger.nulls.examples:jdt-ecj-settings:jar:1.0.0-SNAPSHOT_


## Future

If this is found to be of general interest, perhaps this could move to eclipse.org.  If this happens, it would be imperative to keep it very easy for anyone to contribute via Pull Requests on GitHub (and not, or not you only, eclipse.org Gerrit changes).
