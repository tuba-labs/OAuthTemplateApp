package org.tubalabs.app.etc.db;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
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

    private record NullableRecord(String id, String displayName, String email) {
    }
}
