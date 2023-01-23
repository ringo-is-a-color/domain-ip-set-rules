val scala3Version = "3.2.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "domain-ip-set-rules-data",
    version := "0.1.0-SNAPSHOT",
    scalaVersion := scala3Version,
    libraryDependencies ++= Seq(
      "com.comcast" %% "ip4s-core" % "3.2.0",
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
      "org.xerial" % "sqlite-jdbc" % "3.40.0.0",
      "com.lihaoyi" %% "upickle" % "3.0.0-M1"
    ),
    Compile / PB.targets := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value / "scalapb"
    )
  )
