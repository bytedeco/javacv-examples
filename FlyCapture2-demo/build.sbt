import sbt.Keys._

name := "FlyCapture2-demo"

ThisBuild / version      := "1.5.7"
ThisBuild / scalaVersion := "2.13.8"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

val commonSettings = Seq(
  scalacOptions ++= Seq("-Ymacro-annotations", "-unchecked", "-deprecation", "-Xlint", "-explaintypes"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco"            % "flycapture"               % "2.13.3.31-1.5.7" classifier "",
    "org.bytedeco"            % "flycapture"               % "2.13.3.31-1.5.7" classifier platform,
    "log4j"                   % "log4j"                    % "1.2.17",
    "org.scala-lang"          % "scala-reflect"            % scalaVersion.value,
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.1"
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
    "org.slf4j"                   % "slf4j-api"           % "1.7.36",
    "org.slf4j"                   % "slf4j-log4j12"       % "1.7.36",
    "org.scalafx"                %% "scalafx"             % "18.0.2-R29",
    "org.scalafx"                %% "scalafxml-core-sfx8" % "0.5",
    "org.scalafx"                %% "scalafx-extras"      % "0.7.0"
    )
)

lazy val check_macro = project.in(file("check_macro")).settings(commonSettings: _*)

lazy val examples = project.in(file("examples")).settings(commonSettings: _*).dependsOn(check_macro)

lazy val example_ui = project.in(file("example_ui")).settings(uiSettings: _*).dependsOn(check_macro)
