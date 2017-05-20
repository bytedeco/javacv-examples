// Name of the project
name := "flandmark-demo"

// Project version
version := "1.3"

// Version of Scala used by the project
scalaVersion := "2.12.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xlint")

// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

// @formatter:off
libraryDependencies ++= Seq(
  "org.bytedeco"                 % "javacv"    % "1.3.1",
  "org.bytedeco.javacpp-presets" % "flandmark" % "1.07-1.3" classifier "",
  "org.bytedeco.javacpp-presets" % "flandmark" % "1.07-1.3" classifier platform,
  "org.bytedeco.javacpp-presets" % "opencv"    % "3.1.0-1.3" classifier "",
  "org.bytedeco.javacpp-presets" % "opencv"    % "3.1.0-1.3" classifier platform
)
// @formatter:on

// Used for testing local builds and snapshots of JavaCPP/JavaCV
//resolvers ++= Seq(
//  Resolver.sonatypeRepo("snapshots"),
//  // Use local maven repo for local javacv builds
//  "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
//)

// set the main class for 'sbt run'
mainClass in(Compile, run) := Some("flandmark.Example1")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> " }
