import BuildHelper._
import explicitdeps.ExplicitDepsPlugin.autoImport.moduleFilterRemoveValue
import sbtcrossproject.CrossPlugin.autoImport.crossProject

inThisBuild(
  List(
    organization := "dev.zio",
    homepage := Some(url("https://github.com/zio/zio-intellij")),
    licenses := List("Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
    developers := List(
      Developer(
        "hmemcpy",
        "Igal Tabachnik",
        "hmemcpy@gmail.com",
        url("http://hmemcpy.com")
      )
    ),
    pgpPassphrase := sys.env.get("PGP_PASSWORD").map(_.toArray),
    pgpPublicRing := file("/tmp/public.asc"),
    pgpSecretRing := file("/tmp/secret.asc"),
    scmInfo := Some(
      ScmInfo(
        url("https://github.com/zio/zio-test-intellij/"),
        "scm:git:git@github.com:zio/zio-test-intellij.git"
      )
    )
  )
)

addCommandAlias("fmt", "all scalafmtSbt scalafmt test:scalafmt")
addCommandAlias("check", "all scalafmtSbtCheck scalafmtCheck test:scalafmtCheck")

val zioVersion = "1.0.1"

lazy val root = project
  .in(file("."))
  .settings(
    name := "zio-test-intellij",
    skip in publish := true,
    unusedCompileDependenciesFilter -= moduleFilter("org.scala-js", "scalajs-library")
  )
  .aggregate(
    forJVM,
    forJS
  )

lazy val runner = crossProject(JSPlatform, JVMPlatform)
  .in(file("src"))
  .settings(stdSettings("zio-test-intellij"))
  .settings(crossProjectSettings)
  .settings(buildInfoSettings("zio-test-intellij"))
  .settings(
    libraryDependencies ++= Seq(
      "dev.zio" %% "zio"      % zioVersion,
      "dev.zio" %% "zio-test" % zioVersion
    )
  )

lazy val forJS = runner.js
  .settings(scalaJSUseMainModuleInitializer := true)

lazy val forJVM = runner.jvm
