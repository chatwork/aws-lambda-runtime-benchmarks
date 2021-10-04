import type {APIGatewayProxyHandlerV2} from 'aws-lambda';

const noop: APIGatewayProxyHandlerV2<any> = async (_) => {
    return {
        "message": "noop",
    };
};

export {
    noop,
};
