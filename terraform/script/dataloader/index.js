const {parse} = require('jsonlines');
const fs = require('fs');
const DynamoDB = require('aws-sdk/clients/dynamodb');
const DocumentClient = DynamoDB.DocumentClient;

;(async () => {
    const documentClient = new DocumentClient({
        region: "ap-northeast-1",
    });
    const dynamodb = new DynamoDB({
        region: "ap-northeast-1",
    });

    const jsonlParser = parse();
    const rs = fs.createReadStream('datasheet.jsonl', {encoding: 'utf8'}).pipe(jsonlParser);

    const TableName = 'TestRuntimeBenchmark';
    await dynamodb.updateTable({
        TableName,
        ProvisionedThroughput: {
            ReadCapacityUnits: 100,
            WriteCapacityUnits: 100,
        }
    }).promise();

    for await (const Item of rs) {
        const Key = {
            pk: Item.pk,
            sk: Item.sk,
        };
        const existing = await documentClient.get({
            TableName,
            Key
        }).promise();
        if (existing.Item == undefined) {
            try {
                await documentClient.put({
                    TableName,
                    Item,
                }).promise();
            } catch (e) {
                console.error("Unexpected error.");
                throw e
            }
        } else {
            console.log(`登録済みなのでスキップします: ${JSON.stringify(Key)}`);
        }
    }

    await dynamodb.updateTable({
        TableName,
        ProvisionedThroughput: {
            ReadCapacityUnits: 100,
            WriteCapacityUnits: 1,
        }
    }).promise();
})();
