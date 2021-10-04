package com.chatwork;

import java.util.Map;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;

public class Noop implements RequestHandler<Map<String, Object>, Map<String, String>> {
    @Override
    public Map<String, String> handleRequest(Map<String, Object> input, Context context) {
        return Map.of("message", "noop");
    }
}
