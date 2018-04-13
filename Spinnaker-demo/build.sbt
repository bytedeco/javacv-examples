// @formatter:off

name         := "Spinnaker-demo"
organization := "javacv.examples"

val javacppVersion = "1.4.1"
version      := javacppVersion
scalaVersion := "2.12.5"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.getPlatform
// Libraries with native dependencies
val bytedecoPresetLibs = Seq(
  "spinnaker" -> s"1.10.0.31-1.4.2-SNAPSHOT").flatMap {
  case (lib, ver) => Seq(
    // Add both: dependency and its native binaries for the current `platform`
    "org.bytedeco.javacpp-presets" % lib % ver withSources() withJavadoc(),
    "org.bytedeco.javacpp-presets" % lib % ver classifier platform
  )
}

libraryDependencies ++= Seq(
  "org.bytedeco"            % "javacpp"         % javacppVersion withSources() withJavadoc(),
  "org.bytedeco"            % "javacv"          % javacppVersion withSources() withJavadoc()
) ++ bytedecoPresetLibs

resolvers ++= Seq(
    // Resolver.sonatypeRepo("snapshots"),
    // Use local maven repo for local javacv builds
    Resolver.mavenLocal
)

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> " }