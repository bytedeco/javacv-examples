scalacOptions ++= Seq("-unchecked", "-deprecation")

// sbteclipse plugin can be used to generate Eclipse project from SBT project
addSbtPlugin("com.typesafe.sbteclipse" % "sbteclipse-plugin" % "4.0.0")

// `javacpp` are packaged with maven-plugin packaging, we need to make SBT aware that it should be added to class path.
classpathTypes += "maven-plugin"

// javacpp `Loader` is used to determine `platform` classifier in the project`s `build.sbt`
// We define dependency here (in folder `project`) since it is used by the build itself.
libraryDependencies += "org.bytedeco" % "javacpp" % "1.2"


