import scala.sys.process._

lazy val lambdaPackage = taskKey[Unit]("Build")
lazy val graalVmVersion = "21.2.0"

lazy val root = (project in file(".")).
  enablePlugins(GraalVMNativeImagePlugin).
  settings(
    inThisBuild(List(
      organization := "example",
      scalaVersion := "2.13.6",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "bootstrap",
    libraryDependencies ++= Seq(
      "org.graalvm.nativeimage" % "svm" % graalVmVersion % Provided,
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
      "com.amazonaws" % "aws-lambda-java-events" % "3.9.0",
      "com.fasterxml.jackson.core" % "jackson-core" % "2.12.5",
      "com.fasterxml.jackson.core" % "jackson-databind" % "2.12.5",
      "software.amazon.awssdk" % "dynamodb" % "2.17.29",
    ),
    Universal / mappings := (Universal / mappings).value.filter {
      case (_, path) => !path.contains("org.scala")
    },
    compile / mainClass := Some("com.chatwork.Main"),
    graalVMNativeImageGraalVersion := Some(graalVmVersion),
    graalVMNativeImageOptions ++= List(
      "-H:+ReportExceptionStackTraces",
      "-H:+ReportUnsupportedElementsAtRuntime",
      "-H:IncludeResources=.*\\.properties",
      "-H:EnableURLProtocols=http,https",
      "--initialize-at-build-time=" + Seq(
        "org.slf4j.LoggerFactory",
      ).mkString(","),
      "-H:+ReportExceptionStackTraces",
      // Refer https://github.com/sbt/sbt-native-packager/blob/6c83cf051335e427b9f067bb6d78abc0448e254f/src/sphinx/formats/graalvm-native-image.rst#graalvm-resources
      "-H:ReflectionConfigurationFiles=/opt/graalvm/stage/resources/reflect-config.json",
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
