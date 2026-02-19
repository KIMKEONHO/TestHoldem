package com.holdup.server.service;

import com.holdup.server.table.Table;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 테이블 생성·조회. 액션 핸들러에서 tableId로 테이블을 가져올 때 사용.
 */
public class TableManager {

    private final Map<String, Table> tables = new ConcurrentHashMap<>();

    public Table createTable(String tableId, int maxSeats) {
        return tables.computeIfAbsent(tableId, id -> new Table(id, maxSeats));
    }

    public Table createTable(String tableId, String name, int maxSeats) {
        return tables.computeIfAbsent(tableId, id -> new Table(id, name, maxSeats));
    }

    public Optional<Table> getTable(String tableId) {
        return Optional.ofNullable(tables.get(tableId));
    }

    public void removeTable(String tableId) {
        tables.remove(tableId);
    }

    public boolean exists(String tableId) {
        return tables.containsKey(tableId);
    }
}
