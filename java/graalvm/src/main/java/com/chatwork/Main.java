package com.chatwork;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Main {

    private static HttpResponse<String> getNextInvocation(HttpClient http, String uri) throws IOException, InterruptedException {
        return http.send(
                HttpRequest.newBuilder().uri(URI.create(uri)).GET().build(),
                HttpResponse.BodyHandlers.ofString(UTF_8)
        );
    }

    private static HttpResponse<String> postLambdaApi(HttpClient http, String uri, String body) throws Exception {
        return http.send(
                HttpRequest.newBuilder().uri(URI.create(uri)).POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                HttpResponse.BodyHandlers.ofString(UTF_8)
        );
    }

    public static void main(String[] args) {
        var http = HttpClient.newBuilder().build();
        var context = System.getenv();
        var runtime = context.get("AWS_LAMBDA_RUNTIME_API");
        var objectMapper = new ObjectMapper();
        var mapType = new TypeReference<Map<String, Object>>() {
        };
        var dynamo = DynamoDbClient
                .builder()
                .region(Region.AP_NORTHEAST_1)
                .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
                .httpClient(ApacheHttpClient.builder().tcpKeepAlive(true).build())
                .build();

        while (true) {
            try {
                var invocation = getNextInvocation(http, "http://" + runtime + "/2018-06-01/runtime/invocation/next");
                var requestId = invocation.headers().map().get("lambda-runtime-aws-request-id").get(0);
                var invocationUrl = "http://" + runtime + "/2018-06-01/runtime/invocation/" + requestId;
                try {
                    // 本当は APIGatewayV2HTTPEvent にデコードしたいが 0-arg Creator が見つからず、エラーになる。あるようだが…
                    var event = objectMapper.readValue(invocation.body(), mapType);
                    var rawPath = (String) event.get("rawPath");
                    if (rawPath == null) {
                        throw new Exception("missing \"rawPath\" in event");
                    }
                    switch (rawPath) {
                        case "/noop":
                            postLambdaApi(http, invocationUrl + "/response", "{ \"message\": \"noop\"}");
                            break;
                        case "/getItem":
                            getItem(http, objectMapper, dynamo, invocationUrl, (Map<String, String>) event.get("queryStringParameters"));
                            break;
                        case "/query":
                            query(http, objectMapper, dynamo, invocationUrl, (Map<String, String>) event.get("queryStringParameters"));
                            break;
                        default:
                            throw new Exception("unknown rawPath: " + rawPath);
                    }
                } catch (Exception e) {
                    postLambdaApi(http, invocationUrl + "/error", e.getMessage());
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }
        }

    }

    private static void getItem(HttpClient http, ObjectMapper objectMapper, DynamoDbClient dynamo, String invocationUrl, Map<String, String> queryStringParameter) throws Exception {
        var getItemOutput = dynamo.getItem((e) -> {
            e.tableName("TestRuntimeBenchmark")
                    .key(Map.of(
                            "pk", AttributeValue.builder().s(queryStringParameter.get("pk")).build(),
                            "sk", AttributeValue.builder().s(queryStringParameter.get("sk")).build()
                    ));
        });
        var body = getItemOutput.item().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey, e -> e.getValue().s()
        ));
        var jsonString = objectMapper.writeValueAsString(body);
        postLambdaApi(http,
                invocationUrl + "/response",
                jsonString
        );
    }

    private static void query(HttpClient http, ObjectMapper objectMapper, DynamoDbClient dynamo, String invocationUrl, Map<String, String> queryStringParameter) throws Exception {
        var queryOutput = dynamo.query((e) -> {
            AttributeValue pk = AttributeValue.builder().s(queryStringParameter.get("pk")).build();
            AttributeValue sk = AttributeValue.builder().s(queryStringParameter.get("sk")).build();
            e.tableName("TestRuntimeBenchmark")
                    .keyConditionExpression("pk = :PK")
                    .expressionAttributeValues(
                            Map.of(":PK", pk)
                    )
                    .limit(Integer.parseInt(queryStringParameter.getOrDefault("range", "3"), 10))
                    .exclusiveStartKey(Map.of(
                            "pk", pk,
                            "sk", sk
                    ));
        });
        var items = queryOutput.items().stream().map(item -> {
            return item.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s()));
        }).collect(Collectors.toList());
        var queryResponse = new QueryResponse(items.size(), items);
        var jsonString = objectMapper.writeValueAsString(queryResponse);
        postLambdaApi(http,
                invocationUrl + "/response",
                jsonString
        );
    }

}
