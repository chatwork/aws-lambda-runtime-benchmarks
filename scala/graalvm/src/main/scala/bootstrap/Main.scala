package bootstrap

import io.circe.Printer

import java.net._
import java.net.http._
import java.nio.charset.StandardCharsets.UTF_8
import scala.jdk.CollectionConverters._
import scala.util.Failure
import scala.util.Success
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider
import software.amazon.awssdk.http.apache.ApacheHttpClient
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.{AttributeValue, GetItemRequest, QueryRequest}

final case class Output(message: String)

object Main {

  def main(args: Array[String]): Unit = {
    implicit val printer: Printer = Printer.noSpaces.copy(dropNullValues = true)

    val http = HttpClient.newBuilder().build()
    val context = System.getenv()
    val runtime = context.get("AWS_LAMBDA_RUNTIME_API")

    def getLambdaApi(uri: String): HttpResponse[String] =
      http.send(
        HttpRequest.newBuilder().uri(URI.create(uri)).GET().build(),
        HttpResponse.BodyHandlers.ofString(UTF_8)
      )

    def postLambdaApi(uri: String, response: String): HttpResponse[String] =
      http.send(
        HttpRequest
          .newBuilder()
          .uri(URI.create(uri))
          .POST(HttpRequest.BodyPublishers.ofString(response))
          .build(),
        HttpResponse.BodyHandlers.ofString(UTF_8)
      )

    val dynamo = DynamoDbClient
      .builder()
      .region(Region.AP_NORTHEAST_1)
      .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
      .httpClient(ApacheHttpClient.builder().tcpKeepAlive(true).build())
      .build()

    while (true) {
      try {
        val response: HttpResponse[String] = getLambdaApi(
          s"http://$runtime/2018-06-01/runtime/invocation/next"
        )
        val requestId = response.headers().map().get("lambda-runtime-aws-request-id").asScala.head
        val result = decode[APIGatewayProxyEventV2](response.body()).toTry.map { event =>
          event.rawPath match {
            case "/noop" =>
              Output("noop").asJson.printWith(printer)
            case "/getItem" =>
              getItem(event, dynamo)
            case "/query" =>
              query(event, dynamo)
            case _ =>
              throw new Exception(s"Unknown path")
          }
        }
        result match {
          case Success(response) =>
            postLambdaApi(s"http://$runtime/2018-06-01/runtime/invocation/$requestId/response", response)
          case Failure(e) =>
            postLambdaApi(s"http://$runtime/2018-06-01/runtime/invocation/$requestId/error", e.getMessage)
        }
      } catch {
        case e: Exception =>
          System.err.println(e.getMessage)
      }
    }
  }

  private def getItem(event: APIGatewayProxyEventV2, dynamo: DynamoDbClient)(implicit printer: Printer): String = {
    val s = (for {
      qsp <- event.queryStringParameters
      pk <- qsp.pk
      sk <- qsp.sk
    } yield {
      dynamo.getItem((e: GetItemRequest.Builder) => {
        e.tableName("TestRuntimeBenchmark")
          .key(
            Map(
              "pk" -> AttributeValue.builder().s(pk).build(),
              "sk" -> AttributeValue.builder().s(sk).build()
            ).asJava
          )
      })
    })
    s match {
      case Some(value) =>
        value.item().asScala.view.mapValues(_.s()).toMap.asJson.printWith(printer)
      case None =>
        throw new Exception(s"Missing query string parameters: both pk & sk is required")
    }
  }

  private def query(event: APIGatewayProxyEventV2, dynamo: DynamoDbClient)(implicit printer: Printer): String = {
    val s = (for {
      qsp <- event.queryStringParameters
      pk <- qsp.pk
      sk <- qsp.sk
      range = qsp.range.getOrElse("3")
    } yield {
      dynamo.query((e: QueryRequest.Builder) => {
        e.tableName("TestRuntimeBenchmark")
          .limit(range.toInt)
          .keyConditionExpression("pk = :PK")
          .expressionAttributeValues(
            java.util.Map.of(
              ":PK",
              AttributeValue.builder().s(pk).build()
            )
          )
          .exclusiveStartKey(
            java.util.Map.of(
              "pk",
              AttributeValue.builder().s(pk).build(),
              "sk",
              AttributeValue.builder().s(sk).build()
            )
          )
      })
    })

    s match {
      case Some(awsQueryResponse) =>
        val myQueryResponse = QueryResponse(
          count = awsQueryResponse.count(),
          items = awsQueryResponse.items().asScala.map(e => MyItem(
            pk = e.get("pk").s(),
            sk = e.get("sk").s(),
            attr1 = e.get("attr1").s(),
            attr2 = e.get("attr2").s(),
            attr3 = e.get("attr3").s(),
            attr4 = e.get("attr4").s()
          )).toSeq
        )
        myQueryResponse.asJson.printWith(printer)
      case None =>
        throw new Exception(s"Missing query string parameters: both pk & sk is required")
    }
  }
}
