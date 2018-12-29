// @formatter:off

name         := "opencv-cookbook"
organization := "javacv.examples"

val javacppVersion = "1.4.4-SNAPSHOT"
version      := javacppVersion
scalaVersion := "2.12.8"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.getPlatform
// Libraries with native dependencies
val bytedecoPresetLibs = Seq(
  "opencv" -> s"4.0.0-$javacppVersion",
  "ffmpeg" -> s"4.0.3-$javacppVersion").flatMap {
  case (lib, ver) => Seq(
    // Add both: dependency and its native binaries for the current `platform`
    "org.bytedeco.javacpp-presets" % lib % ver withSources() withJavadoc(),
    "org.bytedeco.javacpp-presets" % lib % ver classifier platform
  )
}

libraryDependencies ++= Seq(
  "org.bytedeco"            % "javacpp"         % javacppVersion withSources() withJavadoc(),
  "org.bytedeco"            % "javacv"          % javacppVersion withSources() withJavadoc(),
  "org.scala-lang.modules" %% "scala-swing" % "2.1.0",
  "junit"                   % "junit"           % "4.12" % "test",
  "com.novocode"            % "junit-interface" % "0.11" % "test"
) ++ bytedecoPresetLibs

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
resolvers += Resolver.mavenLocal

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> " }