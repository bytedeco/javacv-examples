// @formatter:off

import sbt.Keys._

name    := "FlyCapture2-demo"
version := "1.3.4"

scalaVersion := "2.12.4"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

// @formatter:off
val commonSettings = Seq(
  scalaVersion := "2.12.4",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint", "-explaintypes"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco.javacpp-presets" % "flycapture"               % "2.11.3.121-1.4" classifier "",
    "org.bytedeco.javacpp-presets" % "flycapture"               % "2.11.3.121-1.4" classifier platform,
    "log4j"                        % "log4j"                    % "1.2.17",
    "org.scala-lang"               % "scala-reflect"            % scalaVersion.value,
    "org.scala-lang.modules"      %% "scala-parser-combinators" % "1.1.0"
  ),
  resolvers ++= Seq(
    // Resolver.sonatypeRepo("snapshots"),
    // Use local maven repo for local javacv builds
    Resolver.mavenLocal
  ),
  autoCompilerPlugins := true,
  // fork a new JVM for 'run' and 'test:run'
  fork := true,
  // add a JVM option to use when forking a JVM for 'run'
  javaOptions += "-Xmx1G"
)

val uiSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "org.clapper"   %% "grizzled-slf4j"      % "1.3.2",
    "org.slf4j"      % "slf4j-api"           % "1.7.25",
    "org.slf4j"      % "slf4j-log4j12"       % "1.7.25",
    "org.scalafx"   %% "scalafx"             % "8.0.144-R12",
    "org.scalafx"   %% "scalafxml-core-sfx8" % "0.4"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.1" cross CrossVersion.full)
)

lazy val check_macro = project.in(file("check_macro")).settings(commonSettings: _*)

lazy val examples = project.in(file("examples")).settings(commonSettings: _*).dependsOn(check_macro)

lazy val example_ui = project.in(file("example_ui")).settings(uiSettings: _*).dependsOn(check_macro)

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> "}