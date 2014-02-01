name := "opencv2-cookbook"

organization := "javacv.examples"

val javacvVersion = "0.7"

version := javacvVersion + ".1-SNAPSHOT"

scalaVersion := "2.10.3"

scalacOptions ++= Seq("-unchecked", "-deprecation", "-optimize", "-Xlint")

// set the main Java source directory to be <base>/src
scalaSource in Compile <<= baseDirectory(_ / "src")

// set the main Java source directory to be <base>/src
javaSource in Compile <<= baseDirectory(_ / "src")

resourceDirectory <<= baseDirectory(_ / "src")

// set the Scala test source directory to be <base>/test/src
javaSource in Test <<= baseDirectory(_ / "test/src")

// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"

// Determine current platform
val platform = {
  // Determine platform name using code similar to javacpp
  // com.googlecode.javacpp.Loader.java line 60-84
  val jvmName = System.getProperty("java.vm.name").toLowerCase
  var osName  = System.getProperty("os.name").toLowerCase
  var osArch  = System.getProperty("os.arch").toLowerCase
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
  println("platform: "+platformName)
  platformName
}

libraryDependencies ++= Seq(
  "com.googlecode.javacv" % "javacv" % javacvVersion classifier "" classifier platform,
  "org.scala-lang" % "scala-swing" % scalaVersion.value,
  "net.imagej" % "ij" % "1.47v",
  "junit" % "junit" % "4.11" % "test",
  "com.novocode" % "junit-interface" % "0.10" % "test"
)

resolvers ++= Seq(
  "JavaCPP Repository" at "http://maven2.javacpp.googlecode.com/git/",
  "JavaCV Repository" at "http://maven2.javacv.googlecode.com/git/",
  "ImageJ Releases" at "http://maven.imagej.net/content/repositories/releases/"
)

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true

// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"