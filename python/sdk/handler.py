import boto3

dynamodb = boto3.client('dynamodb')
tableName = 'TestRuntimeBenchmark'

def getItem(event, context):
    qsp = event["queryStringParameters"]
    response = dynamodb.get_item(
      TableName=tableName,
      Key={
        'pk': { 'S': qsp["pk"] },
        'sk': { 'S': qsp["sk"] },
      }
    )
    if "Item" not in response:
        return None
    return _unmarshal(response["Item"])

def query(event, context):
    qsp = event["queryStringParameters"]
    response = dynamodb.query(
      TableName=tableName,
      KeyConditionExpression="pk = :PK",
      ExpressionAttributeValues={
        ":PK": { "S": qsp["pk"] }
      },
      Limit = int(qsp.get("range") or "3"),
      ExclusiveStartKey={
        'pk': { 'S': qsp["pk"] },
        'sk': { 'S': qsp["sk"] },
      }
    )
    return {
      "count": len(response["Items"]),
      "items": [_unmarshal(e) for e in response["Items"]]
    }

def _unmarshal(item):
    return {
      'pk': item["pk"]["S"],
      'sk': item["sk"]["S"],
      'attr1': item["attr1"]["S"],
      'attr2': item["attr2"]["S"],
      'attr3': item["attr3"]["S"],
      'attr4': item["attr4"]["S"],
    }
