package org.tubalabs.app.etc.db;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.RecordComponent;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class SqlRecordIntrospector {

    public static final String SQL_COLUMN_SEPARATOR = ",\r\n";
    private final SqlColumnNameResolver columnNameHelper;

    public List<String> columnsFromRecord(@NonNull String tableName,
                                          @NonNull Record record,
                                          @NonNull Collection<String> excludedColumns) {
        return columnsFromShape(tableName, record.getClass().asSubclass(Record.class), excludedColumns);
    }

    public List<String> columnsFromShape(@NonNull String tableName,
                                         @NonNull Class<? extends Record> shape,
                                         @NonNull Collection<String> excludedColumns) {
        return List.copyOf(recordComponents(tableName, shape, excludedColumns).values());
    }

    public LinkedHashMap<String, Object> paramsFromRecord(@NonNull String tableName,
                                                          @NonNull Record record,
                                                          @NonNull Collection<String> excludedInsertColumns) {
        return parameters(tableName, record, excludedInsertColumns);
    }


    public String insertSql(@NonNull String tableName, @NonNull LinkedHashMap<String, Object> params) {
        final Set<String> insertColumns = params.keySet();
        final String csvInsertColumns = csvColumns(insertColumns);
        final String csvPlaceholders = placeholders(insertColumns);
        return """
                INSERT INTO %s 
                (%s)
                VALUES
                (%s)
                """
                .formatted(tableName, csvInsertColumns,
                        csvPlaceholders);
    }


    public String returning(@NonNull Collection<String> returnedColumns) {
        return """
                RETURNING
                %s"""
                .formatted(csvColumns(returnedColumns));
    }

    public String updateWithoutWhere(@NonNull String tableName,
                                     @NonNull LinkedHashMap<String, Object> params) {
        final String setClause = params.keySet().stream()
                .map(column -> column + " = :" + column)
                .collect(Collectors.joining(SQL_COLUMN_SEPARATOR));
        return """
                UPDATE %s
                SET 
                %s
                """
                .formatted(tableName, setClause);
    }

    public String select(@NonNull String tableName,
                         @NonNull List<String> columnsFromRecord) {
        return """
                SELECT 
                %s
                FROM %s
                """
                .formatted(csvColumns(columnsFromRecord), tableName);
    }

    public @NonNull String csvColumns(@NonNull Collection<String> columnsFromRecord) {
        return String.join(SQL_COLUMN_SEPARATOR, columnsFromRecord);
    }

    public String placeholders(@NonNull Collection<String> columnsInOrder) {
        return columnsInOrder.stream()
                .map(column -> ":" + column)
                .collect(Collectors.joining(SQL_COLUMN_SEPARATOR));
    }

    private String columnName(@NonNull String tableName,
                              @NonNull String fieldName) {
        return columnNameHelper.columnName(tableName, fieldName);
    }

    private Map<RecordComponent, String> recordComponents(@NonNull String tableName,
                                                          @NonNull Class<? extends Record> shape,
                                                          @NonNull Collection<String> excludedColumns) {
        final Set<String> normalizedExcludedColumns = excludedColumns.stream()
                .map(column -> columnName(tableName, column))
                .collect(Collectors.toSet());
        return Arrays.stream(shape.getRecordComponents())
                .map(rc -> {
                    final String rcName = columnName(tableName, rc.getName());
                    if (normalizedExcludedColumns.contains(rcName)) {
                        return null;
                    }
                    return Map.entry(rc, rcName);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> right,
                        LinkedHashMap::new));
    }


    private LinkedHashMap<String, Object> parameters(@NonNull String tableName,
                                                     @NonNull Record record,
                                                     @NonNull Collection<String> excludedColumns) {
        final LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();
        final Class<? extends Record> clazz = record.getClass().asSubclass(Record.class);
        for (Map.Entry<RecordComponent, String> e : recordComponents(tableName, clazz, excludedColumns).entrySet()) {
            try {
                parameters.put(e.getValue(), e.getKey().getAccessor().invoke(record));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalArgumentException("Failed to read record values: " + clazz, exception);
            }
        }
        return parameters;
    }


}
