use lambda_runtime::{handler_fn, Context, Error};
use serde_json::{json, Value};

#[tokio::main]
async fn main() -> Result<(), Error> {
    let func = handler_fn(noop);
    lambda_runtime::run(func).await?;
    Ok(())
}

async fn noop(_event: Value, _context: Context) -> Result<Value, Error> {
    Ok(json!({ "message": "noop"}))
}
