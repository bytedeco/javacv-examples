import sbt.Keys._

name := "FlyCapture2-demo"

ThisBuild / version      := "1.5.9"
ThisBuild / scalaVersion := "2.13.11"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

val commonSettings = Seq(
  scalacOptions ++= Seq("-Ymacro-annotations", "-unchecked", "-deprecation", "-Xlint", "-explaintypes"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco"            % "flycapture"               % "2.13.3.31-1.5.9" classifier "",
    "org.bytedeco"            % "flycapture"               % "2.13.3.31-1.5.9" classifier platform,
    "log4j"                   % "log4j"                    % "1.2.17",
    "org.scala-lang"          % "scala-reflect"            % scalaVersion.value,
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0"
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
    "com.typesafe.scala-logging" %% "scala-logging"       % "3.9.5",
    "org.slf4j"                   % "slf4j-api"           % "2.0.7",
    "org.slf4j"                   % "slf4j-log4j12"       % "2.0.7",
    "org.scalafx"                %% "scalafx"             % "20.0.0-R31",
    "org.scalafx"                %% "scalafxml-core-sfx8" % "0.5",
    "org.scalafx"                %% "scalafx-extras"      % "0.8.0"
    )
)

lazy val check_macro = project.in(file("check_macro")).settings(commonSettings: _*)

lazy val examples = project.in(file("examples")).settings(commonSettings: _*).dependsOn(check_macro)

lazy val example_ui = project.in(file("example_ui")).settings(uiSettings: _*).dependsOn(check_macro)
