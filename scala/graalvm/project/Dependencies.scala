import sbt._

object Dependencies {
  val circeVersion = "0.14.1"
  val circeCore = "io.circe" %% "circe-core" % circeVersion
  val circeGeneric = "io.circe" %% "circe-generic" % circeVersion
  val circeParser = "io.circe" %% "circe-parser" % circeVersion
  val circeDeps = Seq(circeCore, circeGeneric, circeParser)

  val awssdkVersion = "2.17.19"
  val awssdkUrlConnectionClient = "software.amazon.awssdk" % "apache-client" % awssdkVersion
  val awssdkDynamoDb = "software.amazon.awssdk" % "dynamodb" % awssdkVersion
  val awsLambdaEvents = "com.amazonaws" % "aws-lambda-java-events" % "3.9.0"
  val awssdkDeps = Seq(
    awssdkUrlConnectionClient,
    awssdkDynamoDb,
    awsLambdaEvents,
  )

  val graalvmSvm = "org.graalvm.nativeimage" % "svm" % "21.1.0" % Provided
}
