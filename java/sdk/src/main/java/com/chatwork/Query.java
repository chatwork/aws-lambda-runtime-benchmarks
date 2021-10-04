package com.chatwork;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.http.apache.ApacheHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;
import java.util.stream.Collectors;

public class Query implements RequestHandler<APIGatewayV2HTTPEvent, QueryResponse> {
    private static final DynamoDbClient dynamo = DynamoDbClient
            .builder()
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(ApacheHttpClient.builder().tcpKeepAlive(true).build())
            .build();

    @Override
    public QueryResponse handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        var queryStringParameter = input.getQueryStringParameters();
        var getItemOutput = dynamo.query((e) -> {
            var pk = AttributeValue.builder().s(queryStringParameter.get("pk")).build();
            var sk = AttributeValue.builder().s(queryStringParameter.get("sk")).build();
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
        var items = getItemOutput.items().stream().map(item -> {
            return item.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s()));
        }).collect(Collectors.toList());

        return new QueryResponse(items.size(), items);
    }
}
