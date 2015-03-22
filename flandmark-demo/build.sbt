// Name of the project
name := "flandmark-demo"

// Project version
version := "0.10"

// Version of Scala used by the project
scalaVersion := "2.11.6"

val javacvVersion = "0.10"

val javacppVersion = "0.10"

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
  "org.bytedeco" % "javacv" % javacvVersion excludeAll(
    ExclusionRule(organization = "org.bytedeco.javacpp-presets"),
    ExclusionRule(organization = "org.bytedeco.javacpp")
    ),
  "org.bytedeco.javacpp-presets" % "flandmark" %
    ("1.07-" + javacppVersion) classifier "" excludeAll ExclusionRule(name = "opencv"),
  "org.bytedeco.javacpp-presets" % "flandmark" %
    ("1.07-" + javacppVersion) classifier platform excludeAll ExclusionRule(name = "opencv"),
  "org.bytedeco.javacpp-presets" % "opencv" % ("2.4.10-" + javacppVersion) classifier "",
  "org.bytedeco.javacpp-presets" % "opencv" % ("2.4.10-" + javacppVersion) classifier platform,
  "org.bytedeco" % "javacpp" % javacppVersion
)

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots"),
  // Use local maven repo for local javacv builds
  "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
)

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true

// Set the prompt (for this build) to include the project id.
shellPrompt in ThisBuild := { state => "sbt:" + Project.extract(state).currentRef.project + "> "}
