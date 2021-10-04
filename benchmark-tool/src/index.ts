import type {Handler} from 'aws-lambda';

import {Lambda} from "@aws-sdk/client-lambda";
import {CloudWatchLogs, FilterLogEventsCommandInput} from "@aws-sdk/client-cloudwatch-logs";
import {FilteredLogEvent} from "@aws-sdk/client-cloudwatch-logs/models";

const logs = new CloudWatchLogs({
    region: 'ap-northeast-1',
})

const lambda = new Lambda({
    region: 'ap-northeast-1',
})

async function sleep(milliseconds: number): Promise<void> {
    return new Promise(resolve => {
        setTimeout(() => {
            resolve(void 0)
        }, milliseconds)
    })
}

// @ts-ignore
const benchmark: Handler<any, number[]> = async (event) => {
    const FunctionName = event.FunctionName ?? "";
    const PayloadJson = event.PayloadJson ?? {};
    const Concurrency = parseInt(event.Concurrency ?? "100", 10);
    const StartTime = event.startTime ?? Date.now();

    // Drop function version
    const parts = FunctionName.split(":");
    const theFunctionName = parts[parts.length - 1];

    for (let i = 0; i < 2; i++) {
        const {MemorySize: initialMemorySize} = await lambda.getFunctionConfiguration({
            FunctionName,
        });
        await Promise.all(Array.from(new Array(Concurrency)).map(() => {
            return lambda.invoke({
                FunctionName,
                Payload: Buffer.from(JSON.stringify(PayloadJson)),
            });
        }));
        await sleep(30_000);

        // Update memory size to shutdown all hot instances
        await lambda.updateFunctionConfiguration({
            FunctionName,
            MemorySize: 1111,
        });
        await lambda.updateFunctionConfiguration({
            FunctionName,
            MemorySize: initialMemorySize,
        });
    }

    const filteredLogs = (await pagingCloudWatchLog({
        logGroupName: `/aws/lambda/${theFunctionName}`,
        filterPattern: '"Duration:"',
        startTime: parseInt(StartTime, 10),
    })).flatMap(e => e.message ? [e.message] : []);

    const durations: number[] = [];
    const initDurations: number[] = [];
    filteredLogs.forEach(log => {
        if (log.includes("Init Duration:")) {
            const init = parseFloat((log.match(/Init Duration: ([^ ]+) ms/) ?? ["", "0"])[1])
            initDurations.push(init);
        } else {
            const billed = parseFloat((log.match(/Duration: ([^ ]+) ms/) ?? ["", "0"])[1]);
            durations.push(billed);
        }
    });

    return {
        count: {
            initDurations: initDurations.length,
            durations: durations.length,
        },
        durations: {
            initDurations,
            durations
        }
    };
};

async function pagingCloudWatchLog(args: FilterLogEventsCommandInput): Promise<Array<FilteredLogEvent>> {
    const output = await logs.filterLogEvents(args);
    const thisResult = output.events ?? [];
    const {
        nextToken,
        ...rest
    } = args;
    if (output.nextToken) {
        const restResult = await pagingCloudWatchLog({
            nextToken: output.nextToken,
            ...rest,
        })
        return [...thisResult, ...restResult];
    }
    return thisResult;
}

export {
    benchmark,
};
