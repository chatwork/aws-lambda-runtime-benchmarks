resource "aws_dynamodb_table" "test_table" {
  name           = "TestRuntimeBenchmark"
  billing_mode   = "PROVISIONED"
  read_capacity  = 100
  write_capacity = 1
  hash_key       = "pk"
  range_key      = "sk"

  attribute {
    name = "pk"
    type = "S"
  }

  attribute {
    name = "sk"
    type = "S"
  }
}

resource "null_resource" "DataLoader" {
  triggers = {
    DataLoaderHash = join(",", [
      filesha256("script/dataloader/index.js"),
      filesha256("script/dataloader/datasheet.jsonl"),
    ])
  }

  provisioner "local-exec" {
    working_dir = "script/dataloader"
    command     = "npm install && node index.js"
  }

  depends_on = [aws_dynamodb_table.test_table]
}
