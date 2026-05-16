package org.tubalabs.app.etc.db;

import lombok.NonNull;
import org.springframework.stereotype.Component;

@Component
public class SqlColumnNameResolver {

    public String columnName(@NonNull String tableName, @NonNull String fieldName) {
        final StringBuilder columnName = new StringBuilder();
        for (int index = 0; index < fieldName.length(); index++) {
            final char current = fieldName.charAt(index);
            if (Character.isUpperCase(current) && index > 0) {
                final char previous = fieldName.charAt(index - 1);
                final boolean previousLowercase = Character.isLowerCase(previous);
                final boolean nextLowercase = index + 1 < fieldName.length()
                        && Character.isLowerCase(fieldName.charAt(index + 1));

                if (previousLowercase || nextLowercase) {
                    columnName.append('_');
                }
            }

            columnName.append(Character.toLowerCase(current));
        }

        return columnName.toString();
    }
}
