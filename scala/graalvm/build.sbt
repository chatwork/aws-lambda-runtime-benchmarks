import Dependencies._
import scala.sys.process._

lazy val lambdaPackage = taskKey[Unit]("Build GraalVM native image")
lazy val graalVmVersion = "21.2.0"

lazy val root = (project in file(".")).
  enablePlugins(GraalVMNativeImagePlugin).
  settings(
    inThisBuild(List(
      organization := "com.chatwork",
      scalaVersion := "2.13.6",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "bootstrap",
    libraryDependencies ++= (circeDeps ++ awssdkDeps ++ Seq(
      graalvmSvm
    )),
    compile / mainClass := Some("bootstrap.Main"),
    graalVMNativeImageGraalVersion := Some(graalVmVersion),
    graalVMNativeImageOptions ++= List(
      "-H:+ReportExceptionStackTraces",
      "-H:+ReportUnsupportedElementsAtRuntime",
      "-H:IncludeResources=.*\\.properties",
      "-H:EnableURLProtocols=http,https",
      "--initialize-at-build-time=" + Seq(
        "org.slf4j.LoggerFactory",
        "scala.Symbol$", // For circe
      ).mkString(","),
      "-H:+ReportExceptionStackTraces",
      // Refer https://github.com/sbt/sbt-native-packager/blob/6c83cf051335e427b9f067bb6d78abc0448e254f/src/sphinx/formats/graalvm-native-image.rst#graalvm-resources
      "-H:ReflectionConfigurationFiles=/opt/graalvm/stage/resources/reflect-config.json",
      "--enable-all-security-services",
      "--no-fallback",
      "--no-server",
      "--allow-incomplete-classpath",
    ),
    lambdaPackage := {
      (GraalVMNativeImage / packageBin).value
      s"chmod 755 target/graalvm-native-image/bootstrap".!
      s"zip target/lambda.zip --junk-paths target/graalvm-native-image/bootstrap".!
    },
  )
