package org.tubalabs.app.etc.db;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SqlColumnNameResolverTest {

    private static final String TABLE_NAME = "ignored";

    private final SqlColumnNameResolver resolver = new SqlColumnNameResolver();

    @ParameterizedTest
    @CsvSource({
            "id,id",
            "displayName,display_name",
            "userId,user_id",
            "clientIP,client_ip",
            "URLValue,url_value",
            "oauth2UserID,oauth2_user_id"
    })
    void resolvesJavaFieldNamesToSqlColumnNames(String fieldName, String expectedColumnName) {
        assertThat(resolver.columnName(TABLE_NAME, fieldName)).isEqualTo(expectedColumnName);
    }
}
