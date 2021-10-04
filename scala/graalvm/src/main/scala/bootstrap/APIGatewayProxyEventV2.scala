package bootstrap

import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

/** HTTP API integration Payload Format version 2.0
  *
  * @see - https://docs.aws.amazon.com/apigateway/latest/developerguide/http-api-develop-integrations-lambda.html
  */
case class APIGatewayProxyEventV2(
    rawPath: String,
    queryStringParameters: Option[MyParam]
)

case class MyParam(
    pk: Option[String],
    sk: Option[String],
    range: Option[String]
)
