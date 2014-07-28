name := "FlyCapture2-demo"

version := "0.9"

scalaVersion := "2.11.2"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xlint")

// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"

// Determine current platform
val platform = {
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

libraryDependencies ++= Seq(
  "org.bytedeco.javacpp-presets" % "flycapture" % "2.6.3.4-0.9" classifier "",
  "org.bytedeco.javacpp-presets" % "flycapture" % "2.6.3.4-0.9" classifier platform,
  "org.bytedeco"                 % "javacpp"    % "0.9"
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  "ImageJ Releases" at "http://maven.imagej.net/content/repositories/releases/",
  // Use local maven repo for local javacv builds
  "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
)

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true

// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> "}