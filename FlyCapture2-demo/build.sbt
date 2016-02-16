import sbt.Keys._

name := "FlyCapture2-demo"

version := "1.1"

scalaVersion := "2.11.7"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

val commonSettings = Seq(
  scalaVersion := "2.11.7",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xlint", "-Yinline-warnings", "-explaintypes"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco.javacpp-presets" % "flycapture" % "2.8.3.1-1.1" classifier "",
    "org.bytedeco.javacpp-presets" % "flycapture" % "2.8.3.1-1.1" classifier platform,
    "log4j"                        % "log4j"         % "1.2.17",
    "org.scala-lang"               % "scala-reflect" % scalaVersion.value,
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
  ),
  resolvers ++= Seq(
    Resolver.sonatypeRepo("snapshots"),
    "ImageJ Releases" at "http://maven.imagej.net/content/repositories/releases/",
    // Use local maven repo for local javacv builds
    "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
  ),
  autoCompilerPlugins := true,
  // fork a new JVM for 'run' and 'test:run'
  fork := true,
  // add a JVM option to use when forking a JVM for 'run'
  javaOptions += "-Xmx1G"
)

val uiSettings = commonSettings ++ Seq(
  libraryDependencies ++= Seq(
    "org.clapper"   %% "grizzled-slf4j" % "1.0.2",
    "org.slf4j"      % "slf4j-api"      % "1.7.16",
    "org.slf4j"      % "slf4j-log4j12"  % "1.7.16",
    "org.scalafx"   %% "scalafx"        % "8.0.60-R9",
    "org.scalafx"   %% "scalafxml-core" % "0.2.1"
  ),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full)
)

lazy val check_macro = project.in(file("check_macro")).settings(commonSettings: _*)

lazy val examples = project.in(file("examples")).settings(commonSettings: _*).dependsOn(check_macro)

lazy val example_ui = project.in(file("example_ui")).settings(uiSettings: _*).dependsOn(check_macro)

// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> "}