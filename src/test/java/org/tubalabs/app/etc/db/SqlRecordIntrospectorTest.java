package org.tubalabs.app.etc.db;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class SqlRecordIntrospectorTest {

    private static final String TABLE_NAME = "nullable_record";
    private static final String ID = "record-1";
    private static final String DISPLAY_NAME = "Petter";

    private final SqlRecordIntrospector sqlRecordIntrospector =
            new SqlRecordIntrospector(new SqlColumnNameResolver());

    @Test
    void paramsFromRecordKeepsNullValues() {
        final NullableRecord record = new NullableRecord(ID, DISPLAY_NAME, null);

        final LinkedHashMap<String, Object> parameters =
                sqlRecordIntrospector.paramsFromRecord(TABLE_NAME, record, Set.of());

        assertThat(parameters.keySet()).containsExactly("id", "display_name", "email");
        assertThat(parameters)
                .containsEntry("id", ID)
                .containsEntry("display_name", DISPLAY_NAME)
                .containsEntry("email", null);
    }

    @Test
    void qualifiesColumnsFromRecordShape() {
        final List<String> columns = sqlRecordIntrospector.qualifiedColumnsFromShape(
                TABLE_NAME, "record", NullableRecord.class, Set.of("email"));

        assertThat(columns).containsExactly("record.id", "record.display_name");
    }

    @Test
    void aliasesColumnsFromRecordShape() {
        final List<String> columns = sqlRecordIntrospector.aliasedColumnsFromShape(
                TABLE_NAME, "record", "nullable", NullableRecord.class, Set.of());

        assertThat(columns).containsExactly(
                "record.id AS nullable_id",
                "record.display_name AS nullable_display_name",
                "record.email AS nullable_email");
    }

    @Test
    void derivesColumnAndAliasFromFieldName() {
        assertThat(sqlRecordIntrospector.columnFromField(TABLE_NAME, "displayName")).isEqualTo("display_name");
        assertThat(sqlRecordIntrospector.qualifiedColumnFromField(TABLE_NAME, "record", "displayName"))
                .isEqualTo("record.display_name");
        assertThat(sqlRecordIntrospector.columnAliasFromField(TABLE_NAME, "nullable", "displayName"))
                .isEqualTo("nullable_display_name");
    }

    private record NullableRecord(String id, String displayName, String email) {
    }
}
