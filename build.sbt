name := "chompdb"

description :="An embeddable distributed BLOB storage library"

organization := "org.alexboisvert"

scalaVersion := "2.9.3"

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= Seq(
  "org.alexboisvert" %% "f1lesystem-core" % "latest.snapshot",
  "junit" % "junit" % "[4.0,)" % "test",
  "org.scalatest" %% "scalatest" % "1.9.1" % "test"
)

