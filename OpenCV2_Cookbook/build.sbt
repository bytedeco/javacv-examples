name := "opencv2-cookbook"
organization := "javacv.examples"

val javacppVersion = "0.11"
version := javacppVersion
scalaVersion := "2.11.6"
scalacOptions ++= Seq("-unchecked", "-deprecation", "-Xlint")

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.getPlatform
// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"
libraryDependencies ++= Seq(
  "org.bytedeco"                 % "javacpp"         % javacppVersion,
  "org.bytedeco"                 % "javacv"          % javacppVersion,
  "org.bytedeco.javacpp-presets" % "opencv" % ("2.4.11-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % ("2.4.11-" + javacppVersion) classifier platform,
  "org.scala-lang.modules"      %% "scala-swing"     % "1.0.1",
  "junit"                        % "junit"           % "4.12" % "test",
  "com.novocode"                 % "junit-interface" % "0.11" % "test"
)

// Used for testing local builds and snapshots of JavaCPP/JavaCV
//resolvers ++= Seq(
//  Resolver.sonatypeRepo("snapshots"),
//  // Use local maven repo for local javacv builds
//  "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
//)

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> "}