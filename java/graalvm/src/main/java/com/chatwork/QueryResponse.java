package com.chatwork;

import java.util.List;
import java.util.Map;

public final class QueryResponse {

    private final int count;
    private final List<Map<String, String>> items;

    public QueryResponse(int count, List<Map<String, String>> items) {
        this.count = count;
        this.items = items;
    }

    public int getCount() {
        return this.count;
    }

    public List<Map<String, String>> getItems() {
        return this.items;
    }
}
