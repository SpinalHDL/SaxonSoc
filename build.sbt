lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.github.spinalhdl",
      scalaVersion := "2.11.12",
      version      := "1.0.0"
    )),
    scalacOptions +=  s"-Xplugin:${new File(baseDirectory.value + "/ext/SpinalHDL/idslplugin/target/scala-2.11/spinalhdl-idsl-plugin_2.11-1.4.1.jar")}",
    scalacOptions += s"-Xplugin-require:idsl-plugin",

    libraryDependencies ++= Seq(
      "org.scalatest" % "scalatest_2.11" % "2.2.1",
      "org.yaml" % "snakeyaml" % "1.8"
    ),
    name := "SaxonSoc",
    scalaSource in Compile := baseDirectory.value / "hardware" / "scala",
    scalaSource in Test    := baseDirectory.value / "test" / "scala"
  ).dependsOn(vexRiscv)

lazy val vexRiscv = RootProject(file("ext/VexRiscv"))

fork := true

connectInput in run := true

