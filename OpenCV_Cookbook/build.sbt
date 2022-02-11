name         := "opencv-cookbook"
organization := "javacv.examples"

val javacppVersion = "1.5.7"
version      := javacppVersion
scalaVersion := "2.13.7"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform

// JavaCPP-Preset libraries with native dependencies
val presetLibs = Seq(
  "opencv"   -> "4.5.5",
  "ffmpeg"   -> "5.0",
  "openblas" -> "0.3.19"
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
  "org.scalafx"            %% "scalafx"         % "17.0.1-R26",
  "org.scalafx"            %% "scalafx-extras"  % "0.4.0",
  "junit"                   % "junit"           % "4.13.2" % "test",
  "com.novocode"            % "junit-interface" % "0.11"   % "test"
) ++ presetLibs

// Add JavaFX dependencies, needed for GUI example
libraryDependencies ++= {
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

resolvers += Resolver.sonatypeRepo("snapshots")

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"
