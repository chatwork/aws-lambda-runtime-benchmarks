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

public class GetItem implements RequestHandler<APIGatewayV2HTTPEvent, Map<String, String>> {
    private static final DynamoDbClient dynamo = DynamoDbClient
            .builder()
            .region(Region.AP_NORTHEAST_1)
            .credentialsProvider(EnvironmentVariableCredentialsProvider.create())
            .httpClient(ApacheHttpClient.builder().tcpKeepAlive(true).build())
            .build();

    @Override
    public Map<String, String> handleRequest(APIGatewayV2HTTPEvent input, Context context) {
        var queryStringParameter = input.getQueryStringParameters();
        var getItemOutput = dynamo.getItem((e) -> {
            e.tableName("TestRuntimeBenchmark")
                    .key(Map.of(
                            "pk", AttributeValue.builder().s(queryStringParameter.get("pk")).build(),
                            "sk", AttributeValue.builder().s(queryStringParameter.get("sk")).build()
                    ));
        });
        if (getItemOutput.hasItem()) {
            Map<String, AttributeValue> item = getItemOutput.item();
            return item.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().s()));
        } else {
            return null;
        }
    }
}
