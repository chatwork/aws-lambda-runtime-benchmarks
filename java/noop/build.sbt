import scala.sys.process._

lazy val lambdaPackage = taskKey[Unit]("Build")

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "example",
      scalaVersion := "2.13.6",
      version := "0.1.0-SNAPSHOT"
    )),
    name := "java-lambda",
    libraryDependencies ++= Seq(
      "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
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
    lambdaPackage := {
      assembly.value
      val assemblyJar = (assembly / assemblyOutputPath).value
      s"cp ${assemblyJar} target/hello.jar".!
    },
  )

