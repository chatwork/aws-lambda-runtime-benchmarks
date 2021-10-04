import type {APIGatewayProxyHandlerV2} from 'aws-lambda';
import type {MyItem, QueryResults} from './types';

const {DynamoDB} = require('@aws-sdk/client-dynamodb');
const {DynamoDBDocument} = require('@aws-sdk/lib-dynamodb');

const https = require('https');
const agent = new https.Agent({
    keepAlive: true,
    maxSockets: Infinity,
});
const dynamodbClient = DynamoDBDocument.from(new DynamoDB({
    convertEmptyValues: true,
    region: 'ap-northeast-1',
    httpOptions: {
        agent,
    }
}));

const TableName = 'TestRuntimeBenchmark';

const getItem: APIGatewayProxyHandlerV2<MyItem> = async (event) => {
    const pk = event.queryStringParameters?.pk;
    const sk = event.queryStringParameters?.sk;
    const result = await dynamodbClient.get({
        TableName,
        Key: {
            pk,
            sk,
        },
    });
    return result.Item as MyItem;
};

const query: APIGatewayProxyHandlerV2<QueryResults> = async (event) => {
    const pk = event.queryStringParameters?.pk;
    const sk = event.queryStringParameters?.sk;
    const Limit = Number(event.queryStringParameters?.range ?? "3");
    const items = await dynamodbClient.query({
        TableName,
        KeyConditionExpression: "pk = :PK",
        ExpressionAttributeValues: {
            ":PK": pk,
        },
        Limit,
        ExclusiveStartKey: {
            pk,
            sk
        },
    });
    console.log("success");

    return {
        count: [{count: items.Count ?? 0}],
        items: items.Items as Array<MyItem>
    };
};

export {
    getItem,
    query,
};
