scalacOptions ++= Seq("-unchecked", "-deprecation")

// `javacpp` are packaged with maven-plugin packaging, we need to make SBT aware that it should be added to class path.
classpathTypes += "maven-plugin"

// javacpp `Loader` is used to determine `platform` classifier in the project`s `build.sbt`
// We define dependency here (in folder `project`) since it is used by the build itself.
libraryDependencies += "org.bytedeco" % "javacpp" % "1.5.7"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
  //  // Use local maven repo for local javacv builds
  //  "Local Maven Repository" at "file:///" + Path.userHome.absolutePath + "/.m2/repository"
)
