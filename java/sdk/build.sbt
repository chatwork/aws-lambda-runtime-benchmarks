import scala.sys.process._

lazy val lambdaPackage = taskKey[Unit]("Build")

lazy val root = (project in file(".")).
  enablePlugins(NativeImagePlugin).
  settings(
    inThisBuild(List(
      organization := "example",
      scalaVersion := "2.13.6",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "java-lambda",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
      "com.amazonaws" % "aws-lambda-java-events" % "3.10.0",
      "software.amazon.awssdk" % "dynamodb" % "2.17.50" excludeAll(
        ExclusionRule("software.amazon.awssdk", "netty-nio-client"),
        ExclusionRule("software.amazon.awssdk", "url-connection-client"),
      )
    ),
    assembly / assemblyOption := (assembly / assemblyOption).value.copy(includeScala = false),
    assembly / assemblyMergeStrategy := {
      case "codegen-resources/customization.config" => MergeStrategy.discard
      case "codegen-resources/paginators-1.json" => MergeStrategy.discard
      case "codegen-resources/service-2.json" => MergeStrategy.discard
      case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
      case "module-info.class" => MergeStrategy.discard
      case "software/amazon/awssdk/global/handlers/execution.interceptors" => MergeStrategy.discard
      case otherFile =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(otherFile)
    },
    nativeImageOptions ++= List(
      "-H:IncludeResources=.*\\.properties",
      "-H:EnableURLProtocols=http,https",
      "--no-fallback",
      "--no-server",
    ),
    lambdaPackage := {
      assembly.value
      val assemblyJar = (assembly / assemblyOutputPath).value
      s"cp ${assemblyJar} target/hello.jar".!
    },
  )

