name         := "opencv-cookbook"
organization := "javacv.examples"

val javacppVersion = "1.5.12"
version      := javacppVersion
scalaVersion := "3.3.6"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint", "-source:3.3-migration")

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

// JavaCPP-Preset libraries with native dependencies
val presetLibs = Seq(
  "opencv"   -> "4.11.0",
  "ffmpeg"   -> "7.1.1",
  "openblas" -> "0.3.30"
).flatMap { case (lib, ver) =>
  Seq(
    "org.bytedeco" % lib % s"$ver-$javacppVersion",
    "org.bytedeco" % lib % s"$ver-$javacppVersion" classifier platform
  )
}

libraryDependencies ++= Seq(
  "org.bytedeco"            % "javacpp"         % javacppVersion,
  "org.bytedeco"            % "javacpp"         % javacppVersion classifier platform,
  "org.bytedeco"            % "javacv"          % javacppVersion,
  "org.scala-lang.modules" %% "scala-swing"     % "3.0.0",
  "org.scalafx"            %% "scalafx"         % "24.0.0-R35",
  "org.scalafx"            %% "scalafx-extras"  % "0.11.0",
  "junit"                   % "junit"           % "4.13.2" % "test",
  "com.novocode"            % "junit-interface" % "0.11"   % "test"
) ++ presetLibs

resolvers += Resolver.sonatypeCentralSnapshots

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"