import sbt.Keys._

name := "FlyCapture2-demo"

version := "0.9-SNAPSHOT"

val commonSettings = Seq(
  scalaVersion := "2.11.2",
  scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xlint", "-Yinline-warnings", "-explaintypes"),
  // Some dependencies like `javacpp` are packaged with maven-plugin packaging
  classpathTypes += "maven-plugin",
  libraryDependencies ++= Seq(
    "org.bytedeco.javacpp-presets" % "flycapture"    % "2.6.3.4-0.9" classifier "",
    "org.bytedeco.javacpp-presets" % "flycapture"    % "2.6.3.4-0.9" classifier platform,
    "log4j"                        % "log4j"         % "1.2.17",
    "org.scala-lang"               % "scala-reflect" % scalaVersion.value,
    "org.scala-lang.modules"      %% "scala-parser-combinators" % "1.0.2"
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
    "org.slf4j"      % "slf4j-api"      % "1.7.7",
    "org.slf4j"      % "slf4j-log4j12"  % "1.7.7",
    "org.scalafx"   %% "scalafx"        % "8.0.20-R6",
    "org.scalafx"   %% "scalafxml-core" % "0.2.1",
    "org.controlsfx" % "controlsfx"     % "8.0.6_20"
  ),
//  incOptions := incOptions.value.withNameHashing(false),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.0.1" cross CrossVersion.full)
)

lazy val check_macro = project.in(file("check_macro")).settings(commonSettings: _*)

lazy val examples = project.in(file("examples")).settings(commonSettings: _*).dependsOn(check_macro)

lazy val example_ui = project.in(file("example_ui")).settings(uiSettings: _*).dependsOn(check_macro)

// Determine current platform
lazy val platform = {
  // Determine platform name using code similar to javacpp
  // com.googlecode.javacpp.Loader.java line 60-84
  val jvmName = System.getProperty("java.vm.name").toLowerCase
  var osName = System.getProperty("os.name").toLowerCase
  var osArch = System.getProperty("os.arch").toLowerCase
  if (jvmName.startsWith("dalvik") && osName.startsWith("linux")) {
    osName = "android"
  } else if (jvmName.startsWith("robovm") && osName.startsWith("darwin")) {
    osName = "ios"
    osArch = "arm"
  } else if (osName.startsWith("mac os x")) {
    osName = "macosx"
  } else {
    val spaceIndex = osName.indexOf(' ')
    if (spaceIndex > 0) {
      osName = osName.substring(0, spaceIndex)
    }
  }
  if (osArch.equals("i386") || osArch.equals("i486") || osArch.equals("i586") || osArch.equals("i686")) {
    osArch = "x86"
  } else if (osArch.equals("amd64") || osArch.equals("x86-64") || osArch.equals("x64")) {
    osArch = "x86_64"
  } else if (osArch.startsWith("arm")) {
    osArch = "arm"
  }
  val platformName = osName + "-" + osArch
  println("platform: " + platformName)
  platformName
}

// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> "}