// Name of the project
name := "flandmark-demo"

// Project version
version := "1.5.7"

// Version of Scala used by the project
scalaVersion := "3.3.0"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xlint")

// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

// @formatter:off
libraryDependencies ++= Seq(
  "org.bytedeco" % "javacpp"   % "1.5.9"        classifier platform,
  "org.bytedeco" % "javacv"    % "1.5.9",
  "org.bytedeco" % "flandmark" % "1.07-1.5.8"   classifier "",
  "org.bytedeco" % "flandmark" % "1.07-1.5.8"   classifier platform,
  "org.bytedeco" % "openblas"  % "0.3.23-1.5.9" classifier "",
  "org.bytedeco" % "openblas"  % "0.3.23-1.5.9" classifier platform,
  "org.bytedeco" % "opencv"    % "4.7.0-1.5.9"  classifier "",
  "org.bytedeco" % "opencv"    % "4.7.0-1.5.9"  classifier platform
)
// @formatter:on

// Used for testing local builds and snapshots of JavaCPP/JavaCV
//resolvers ++= Seq(
//  Resolver.sonatypeRepo("snapshots"),
//  // Use local maven repo for local javacv builds
//  "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
//)

// set the main class for 'sbt run'
Compile / run / mainClass := Some("flandmark.Example1")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true
