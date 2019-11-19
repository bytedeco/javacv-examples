// @formatter:off

import sbt.Keys._

name    := "FlyCapture2-demo"
version := "1.5.2"

scalaVersion := "2.13.1"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

// @formatter:off
val commonSettings = Seq(
  scalaVersion := "2.13.1",
  scalacOptions ++= Seq("-Ymacro-annotations", "-unchecked", "-deprecation", "-Xlint", "-explaintypes"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco"             % "flycapture"               % "2.13.3.31-1.5.2" classifier "",
    "org.bytedeco"             % "flycapture"               % "2.13.3.31-1.5.2" classifier platform,
    "log4j"                    % "log4j"                    % "1.2.17",
    "org.scala-lang"           % "scala-reflect"            % scalaVersion.value,
    "org.scala-lang.modules"  %% "scala-parser-combinators" % "1.1.2",
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

// Determine OS version of JavaFX binaries
lazy val osName = System.getProperty("os.name") match {
  case n if n.startsWith("Linux")   => "linux"
  case n if n.startsWith("Mac")     => "mac"
  case n if n.startsWith("Windows") => "win"
  case _ => throw new Exception("Unknown platform!")
}

lazy val javaFXModules = Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")

val uiSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "org.clapper"   %% "grizzled-slf4j"      % "1.3.4",
    "org.slf4j"      % "slf4j-api"           % "1.7.29",
    "org.slf4j"      % "slf4j-log4j12"       % "1.7.29",
    "org.scalafx"   %% "scalafx"             % "12.0.2-R18",
    "org.scalafx"   %% "scalafxml-core-sfx8" % "0.5",
    "org.scalafx"   %% "scalafx-extras"      % "0.3.2"
  ),
  libraryDependencies ++= javaFXModules.map(m => "org.openjfx" % s"javafx-$m" % "12.0.2" classifier osName),
)

lazy val check_macro = project.in(file("check_macro")).settings(commonSettings: _*)

lazy val examples = project.in(file("examples")).settings(commonSettings: _*).dependsOn(check_macro)

lazy val example_ui = project.in(file("example_ui")).settings(uiSettings: _*).dependsOn(check_macro)

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> "}