package org.tubalabs.app.etc.db;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
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

    public String columnFromField(@NonNull String tableName, @NonNull String fieldName) {
        return columnName(tableName, fieldName);
    }

    public String qualifiedColumnFromField(@NonNull String tableName,
                                           @NonNull String tableAlias,
                                           @NonNull String fieldName) {
        return tableAlias + "." + columnFromField(tableName, fieldName);
    }

    public String columnAliasFromField(@NonNull String tableName,
                                       @NonNull String columnAliasPrefix,
                                       @NonNull String fieldName) {
        return columnAliasPrefix + "_" + columnFromField(tableName, fieldName);
    }

    public List<String> qualifiedColumnsFromShape(@NonNull String tableName,
                                                  @NonNull String tableAlias,
                                                  @NonNull Class<? extends Record> shape,
                                                  @NonNull Collection<String> excludedColumns) {
        return recordComponents(tableName, shape, excludedColumns).values().stream()
                .map(column -> tableAlias + "." + column)
                .toList();
    }

    public List<String> aliasedColumnsFromShape(@NonNull String tableName,
                                                @NonNull String tableAlias,
                                                @NonNull String columnAliasPrefix,
                                                @NonNull Class<? extends Record> shape,
                                                @NonNull Collection<String> excludedColumns) {
        return recordComponents(tableName, shape, excludedColumns).values().stream()
                .map(column -> tableAlias + "." + column + " AS " + columnAliasPrefix + "_" + column)
                .toList();
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
                .map(recordComponent -> {
                    final String recordComponentColumnName = columnName(tableName, recordComponent.getName());
                    if (normalizedExcludedColumns.contains(recordComponentColumnName)) {
                        return null;
                    }
                    return Map.entry(recordComponent, recordComponentColumnName);
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
        final Class<? extends Record> recordClass = record.getClass().asSubclass(Record.class);
        for (Map.Entry<RecordComponent, String> entry :
                recordComponents(tableName, recordClass, excludedColumns).entrySet()) {
            try {
                parameters.put(entry.getValue(), entry.getKey().getAccessor().invoke(record));
            } catch (ReflectiveOperationException exception) {
                throw new IllegalArgumentException("Failed to read record values: " + recordClass, exception);
            }
        }
        return parameters;
    }
}
