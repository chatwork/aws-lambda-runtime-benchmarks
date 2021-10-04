use dynamodb::Client as DynamoDbClient;
use lambda_runtime::{handler_fn, Context, Error};
use aws_lambda_events::event::apigw::ApiGatewayV2httpRequest;
use log::LevelFilter;
use serde::Serialize;
use serde_json::{json, Value};
use simple_logger::SimpleLogger;
use dynamodb::model::AttributeValue;

#[tokio::main]
async fn main() -> Result<(), Error> {
    SimpleLogger::new().with_level(LevelFilter::Info).init().unwrap();
    let client = SharedClient {
        client: DynamoDbClient::from_env(),
    };
    lambda_runtime::run(handler_fn(|req, ctx| client.call(req, ctx))).await?;
    Ok(())
}

struct SharedClient {
    client: DynamoDbClient,
}

impl SharedClient {
    pub async fn call(&self, event: ApiGatewayV2httpRequest, _: Context) -> Result<Value, Error> {
        let raw_path = &*(event.raw_path.clone().unwrap());
        match raw_path {
            "/noop" => Ok(json!({
                "message": "noop"
            })),
            "/getItem" => self.get_item(event).await,
            "/query" => self.query(event).await,
            _ => Err(Error::from("不明なパス")),
        }
    }

    async fn get_item(&self, event: ApiGatewayV2httpRequest) -> Result<Value, Error> {
        let qs = event.query_string_parameters;
        let get_item_output = self.client
            .get_item()
            .table_name("TestRuntimeBenchmark")
            .key("pk", AttributeValue::S(qs.get("pk").unwrap().to_string()))
            .key("sk", AttributeValue::S(qs.get("sk").unwrap().to_string()))
            .send()
            .await?;
        match get_item_output.item {
            None => return Err(Error::from("アイテムが見つからなかった")),
            Some(x) => {
                let response = MyItem {
                    pk: x.get("pk").and_then(|a| a.as_s().ok()).unwrap().clone(),
                    sk: x.get("pk").and_then(|a| a.as_s().ok()).unwrap().clone(),
                    attr1: x.get("attr1").and_then(|a| a.as_s().ok()).unwrap().clone(),
                    attr2: x.get("attr2").and_then(|a| a.as_s().ok()).unwrap().clone(),
                    attr3: x.get("attr3").and_then(|a| a.as_s().ok()).unwrap().clone(),
                    attr4: x.get("attr4").and_then(|a| a.as_s().ok()).unwrap().clone(),
                };
                return Ok(json!(response));
            }
        };
    }

    async fn query(&self, event: ApiGatewayV2httpRequest) -> Result<Value, Error> {
        let qs = event.query_string_parameters;
        let range = qs.get("range").unwrap_or(&"3".to_string()).parse::<i32>().unwrap();
        let query_output = self.client
            .query()
            .table_name("TestRuntimeBenchmark")
            .limit(range)
            .key_condition_expression("pk = :PK")
            .expression_attribute_values(":PK", AttributeValue::S(qs.get("pk").unwrap().to_string()))
            .exclusive_start_key("pk", AttributeValue::S(qs.get("pk").unwrap().to_string()))
            .exclusive_start_key("sk", AttributeValue::S(qs.get("sk").unwrap().to_string()))
            .send()
            .await?;
        let my_items: Vec<MyItem> = query_output.items.unwrap().iter().map(|x| {
            return MyItem {
                pk: x.get("pk").and_then(|a| a.as_s().ok()).unwrap().clone(),
                sk: x.get("sk").and_then(|a| a.as_s().ok()).unwrap().clone(),
                attr1: x.get("attr1").and_then(|a| a.as_s().ok()).unwrap().clone(),
                attr2: x.get("attr2").and_then(|a| a.as_s().ok()).unwrap().clone(),
                attr3: x.get("attr3").and_then(|a| a.as_s().ok()).unwrap().clone(),
                attr4: x.get("attr4").and_then(|a| a.as_s().ok()).unwrap().clone(),
            };
        }).collect();
        return Ok(json!({
                    "count": my_items.len(),
                    "items": my_items
                }));
    }
}

#[derive(Serialize)]
pub struct MyItem {
    pk: String,
    sk: String,
    attr1: String,
    attr2: String,
    attr3: String,
    attr4: String,
}
