// Name of the project
name := "Pylon-demo"

// Project version
version := "0.1"

// Version of Scala used by the project
scalaVersion := "2.11.7"

val javacppVersion = "1.2-SNAPSHOT"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.getPlatform

libraryDependencies ++= Seq(
  "org.bytedeco" % "javacpp" % javacppVersion,
  // "org.bytedeco" % "javacv" % javacppVersion,
  "org.bytedeco.javacpp-presets" % "pylon" % ("5.0.0-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "pylon" % ("5.0.0-" + javacppVersion) classifier platform,
  // Tests
  "junit"                        % "junit"           % "4.12" % "test",
  "com.novocode"                 % "junit-interface" % "0.11" % "test"
)

// Used for testing local builds and snapshots of JavaCPP/JavaCV
resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  // Use local maven repo for local javacv builds
  "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
)

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> " }
