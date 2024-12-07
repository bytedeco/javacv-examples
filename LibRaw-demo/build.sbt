// Name of the project
name := "LibRaw-demo"

// Project version
version := "1.5.11"

// Version of Scala used by the project
scalaVersion := "3.5.2"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-explain",
  "-explain-types",
  "-rewrite",
  "-source:3.5-migration",
  "-Wvalue-discard",
  "-Wunused:all"
)

// Some dependencies like `javacpp` are packaged with maven-plugin packaging
classpathTypes += "maven-plugin"

libraryDependencies += "net.imagej" % "ij" % "1.54m"

// Platform classifier for native library dependencies
lazy val platform = org.bytedeco.javacpp.Loader.Detector.getPlatform
libraryDependencies ++= Seq(
  "org.bytedeco" % "libraw" % "0.21.2-1.5.11" withSources () withJavadoc (),
  "org.bytedeco" % "libraw" % "0.21.2-1.5.11" classifier platform
)

// Used for testing local builds and snapshots of JavaCPP/JavaCV
resolvers ++= Resolver.sonatypeOssRepos("snapshots")
resolvers += Resolver.mavenLocal

// set the main class for 'sbt run'
//Compile / run / mainClass := Some("libraw.examples.LibRawDemo4J")

// Fork a new JVM for 'run' and 'test:run', to avoid JavaFX double initialization problems
fork := true
