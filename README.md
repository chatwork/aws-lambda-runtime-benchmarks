# aws-lambda-runtime-benchmarks

AWS Lambda の各ランタイムを比較するためのデモコードです。

- `benchmark-tool` ディレクトリには、Lambda 関数にリクエストを投げて、CloudWatch Logs を収集する Lambda 関数があります。
- `test-events` ディレクトリには、Lambda コンソールでテスト実行するためのテストイベントJSONが格納してあります。
- その他の `java` や `python` などのディレクトリには、各ランタイムでの Lambda 関数とプロビジョニングする Serverless Framework の設定が入っています。

## 前提

- AWS リージョンは ap-northeast-1 を利用します。
- ap-northeast-1 に `TestRuntimeBenchmark` という名前の DynamoDB テーブルを作成しておいてください。
    - `S` 型の `pk` という名前のハッシュキー、`S` 型の `sk` という名前のレンジキーが必要です。
- Lambda 関数のアーティファクトの作成やプロビジョニングのため、以下をインストールしておいてください。 
    - Node.js 12 以上 
    - (Pythonプロジェクトを試すなら）Python 3 
    - (Java、Scalaプロジェクトを試すなら）Java 11、[sbt](https://www.scala-sbt.org/1.x/docs/ja/Setup.html)
    - (Rust プロジェクトを試すなら）[rustup](https://doc.rust-lang.org/cargo/getting-started/installation.html)
    
## 各サンプルプロジェクトのデプロイ方法

1. 各サンプルプロジェクトのディレクトリに移動します。例えば `java/sdk/` です。
2. 各種ライブラリをインストールするため、`npm install` を実行します。
3. Lambda 関数をプロビジョニングするため、`npm run deploy` を実行します。
4. 関数を使い終わってもう不要になったら `npm run remove` を実行します。

## benchmark-tool の利用方法

1. `benchmark-tool` に移動します。
2. 各種ライブラリをインストールするため、`npm install` を実行します。
3. Lambda 関数をプロビジョニングするため、`npm run deploy` を実行します。
4. Web ブラウザで AWS Lambda コンソールを開きます。
5. デプロイしたベンチマーク用 Lambda関数（例えば `RuntimeBenchmark-TypeScript-Bench-dev-benchmark`）のテストコンソールを開きます。
6. テストイベントとして、以下のような JSON を作成し、テスト実行します。
    ```json5
    {
      "FunctionName": "RuntimeBenchmark-TypeScript-SDK-dev-getItem",
      "Concurrency": "100",
      "PayloadJson": { 
          /* PayloadJson の中身は test-events ディレクトリから取得します */
         "rawPath": "/getItem",
         "queryStringParameters": {
               "pk": "1",
               "sk": "1"
         },
         ...
      }
    }
    ```
   - `FunctionName`: Lambda 関数名。ARN ではありません。
   - `Concurrency`: Lambda の並列実行を試みる回数。通常、100〜200くらいを指定します。
   - `PayLoadJson`: 指定した関数に与える JSON オブジェクト。
7. テスト結果は JSON 形式で、指定された Lambda について、次の測定結果を含みます。
    - `count.initDurations`: コールドスタートした回数 
    - `count.durations`: 実際の処理を行った回数 
    - `durations.initDurations`: コールドスタートにかかった時間（ミリ秒）
    - `durations.durations`: 実際の処理にかかった時間（ミリ秒）
