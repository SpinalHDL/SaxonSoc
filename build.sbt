val spinalVersion = "dev"

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.spinalhdl",
      scalaVersion := "2.11.12",
      version      := "1.0.0"
    )),
    scalacOptions +=  s"-Xplugin:${new File(baseDirectory.value + s"/ext/SpinalHDL/idslplugin/target/scala-2.11/spinalhdl-idsl-plugin_2.11-$spinalVersion.jar")}",
    scalacOptions += s"-Xplugin-require:idsl-plugin",

    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.5",
      "org.yaml" % "snakeyaml" % "1.8",
      "org.scalactic" %% "scalactic" % "3.2.10"
    ),
    name := "SaxonSoc",
    scalaSource in Compile := baseDirectory.value / "hardware" / "scala",
    scalaSource in Test    := baseDirectory.value / "test" / "scala"
  ).dependsOn(vexRiscv)

lazy val vexRiscv = RootProject(file("ext/VexRiscv"))

fork := true

connectInput in run := true

