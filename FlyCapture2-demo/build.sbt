import sbt.Keys._

name := "FlyCapture2-demo"

ThisBuild / version      := "1.5.6"
ThisBuild / scalaVersion := "2.13.7"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

val commonSettings = Seq(
  scalacOptions ++= Seq("-Ymacro-annotations", "-unchecked", "-deprecation", "-Xlint", "-explaintypes"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco"            % "flycapture"               % "2.13.3.31-1.5.6" classifier "",
    "org.bytedeco"            % "flycapture"               % "2.13.3.31-1.5.6" classifier platform,
    "log4j"                   % "log4j"                    % "1.2.17",
    "org.scala-lang"          % "scala-reflect"            % scalaVersion.value,
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.1.0"
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

lazy val javaFXModules = {
  // Determine OS version of JavaFX binaries
  lazy val osName = System.getProperty("os.name") match {
    case n if n.startsWith("Linux")   => "linux"
    case n if n.startsWith("Mac")     => "mac"
    case n if n.startsWith("Windows") => "win"
    case _                            => throw new Exception("Unknown platform!")
  }
  Seq("base", "controls", "fxml", "graphics", "media", "swing", "web")
    .map(m => "org.openjfx" % s"javafx-$m" % "17.0.1" classifier osName)
}

val uiSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "com.typesafe.scala-logging" %% "scala-logging"       % "3.9.4",
    "org.slf4j"                   % "slf4j-api"           % "1.7.32",
    "org.slf4j"                   % "slf4j-log4j12"       % "1.7.32",
    "org.scalafx"                %% "scalafx"             % "17.0.1-R26",
    "org.scalafx"                %% "scalafxml-core-sfx8" % "0.5",
    "org.scalafx"                %% "scalafx-extras"      % "0.4.0"
  ),
  libraryDependencies ++= javaFXModules
)

lazy val check_macro = project.in(file("check_macro")).settings(commonSettings: _*)

lazy val examples = project.in(file("examples")).settings(commonSettings: _*).dependsOn(check_macro)

lazy val example_ui = project.in(file("example_ui")).settings(uiSettings: _*).dependsOn(check_macro)
