import Dependencies._
import BuildSettings._
import com.lucidchart.sbt.scalafmt.ScalafmtCorePlugin.autoImport._
import sbt.librarymanagement.Resolver

lazy val commonDeps = Seq(logback, scalaTest, scalaCheck)
lazy val akkaDeps = Seq(
  akkaSlf4J,
  akkaCluster,
  akkaTyped,
  akkaClusterTyped,
  akkaStream,
  akkaManagement,
  akkaManagementClusterBootstrap,
  akkaServiceDiscoveryDNS,
  akkaServiceDiscoveryKubernetes,
  akkaClusterHttpManagement,
  akkaClusterHttpManagement,
  akkaTestkitTyped,
  akkaCors,
  akkaClusterDowning
)

lazy val akkaPersistenceDeps =
  Seq(akkaPersistence,
      akkaClusterSharding,
      akkaPersistenceTyped,
      akkaClusterShardingTyped,
      akkaPersistenceCassandra,
      cassandraLauncher)

lazy val akkaHttpDeps = Seq(akkaHttp, akkaHttpTestkit, akkaHttpCirce)
lazy val circeDeps = Seq(circe, circeGeneric, circeParser)

lazy val slickDeps = Seq(slick, slickHikaryCP, postgres, h2)

lazy val scalafmtSettings = Seq(
  scalafmtOnCompile in ThisBuild := true,
  scalafmtTestOnCompile in ThisBuild := true
)

lazy val dockerSettings = Seq(
  Docker / maintainer := "Juan Marin Otero",
  dockerBaseImage := "openjdk:jre-alpine",
  dockerRepository := Some("hmda")
)

lazy val packageSettings = Seq(
  // removes all jar mappings in universal and appends the fat jar
  mappings in Universal := {
    // universalMappings: Seq[(File,String)]
    val universalMappings = (mappings in Universal).value
    val fatJar = (assembly in Compile).value
    // removing means filtering
    val filtered = universalMappings filter {
      case (_, fileName) => !fileName.endsWith(".jar")
    }
    // add the fat jar
    filtered :+ (fatJar -> ("lib/" + fatJar.getName))
  },
  // the bash scripts classpath only needs the fat jar
  scriptClasspath := Seq((assemblyJarName in assembly).value)
)

lazy val `hmda-root` = (project in file("."))
  .settings(hmdaBuildSettings: _*)
  .aggregate(common, `hmda-platform`, `check-digit`, `institutions-api`)

lazy val common = (project in file("common"))
  .settings(hmdaBuildSettings: _*)
  .settings(
    PB.targets in Compile := Seq(
      scalapb.gen() -> (sourceManaged in Compile).value
    ),
    Seq(
      libraryDependencies ++= commonDeps ++ akkaDeps ++ akkaPersistenceDeps ++ akkaHttpDeps ++ circeDeps ++ slickDeps
    )
  )

lazy val `hmda-platform` = (project in file("hmda"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.HmdaPlatform"),
      assemblyJarName in assembly := "hmda2.jar",
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      libraryDependencies ++= akkaPersistenceDeps
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")

lazy val `check-digit` = (project in file("check-digit"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      },
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")

lazy val `institutions-api` = (project in file("institutions-api"))
  .enablePlugins(JavaServerAppPackaging,
                 sbtdocker.DockerPlugin,
                 AshScriptPlugin)
  .settings(hmdaBuildSettings: _*)
  .settings(
    Seq(
      mainClass in Compile := Some("hmda.institution.HmdaInstitutionApi"),
      assemblyMergeStrategy in assembly := {
        case "application.conf"                      => MergeStrategy.concat
        case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
        case x =>
          val oldStrategy = (assemblyMergeStrategy in assembly).value
          oldStrategy(x)
      },
      assemblyJarName in assembly := {
        s"${name.value}.jar"
      }
    ),
    scalafmtSettings,
    dockerSettings,
    packageSettings
  )
  .dependsOn(common % "compile->compile;test->test")
