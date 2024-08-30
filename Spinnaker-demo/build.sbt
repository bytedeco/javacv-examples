name         := "Spinnaker-demo"
organization := "javacv.examples"

val javacppVersion = "1.5.11-SNAPSHOT"
version      := javacppVersion
scalaVersion := "3.5.0"
scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-explain",
  "-explain-types",
  "-rewrite",
  "-source:3.3-migration",
//  "-Wvalue-discard",
  "-Wunused:all"
)

// Platform classifier for native library dependencies
val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform
// Libraries with native dependencies
val bytedecoPresetLibs = Seq(
  "spinnaker" -> s"4.0.0.116-$javacppVersion"
).flatMap {
  case (lib, ver) => Seq(
      // Add both: dependency and its native binaries for the current `platform`
      "org.bytedeco" % lib % ver withSources () withJavadoc (),
      "org.bytedeco" % lib % ver classifier platform
    )
}

libraryDependencies ++= Seq(
  "org.bytedeco" % "javacpp" % javacppVersion withSources () withJavadoc (),
  "org.bytedeco" % "javacv"  % javacppVersion withSources () withJavadoc ()
) ++ bytedecoPresetLibs

resolvers ++= Resolver.sonatypeOssRepos("snapshots")
resolvers += Resolver.mavenLocal

autoCompilerPlugins := true

// fork a new JVM for 'run' and 'test:run'
fork := true
// add a JVM option to use when forking a JVM for 'run'
javaOptions += "-Xmx1G"
