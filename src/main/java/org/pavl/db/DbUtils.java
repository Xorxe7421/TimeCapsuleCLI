package org.pavl.db;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class DbUtils {

    public static void populateStatement(PreparedStatement preparedStatement, Object... parameters) throws SQLException {
        for (int i = 1; i <= parameters.length; i++) {
            preparedStatement.setObject(i, parameters[i - 1]);
        }
    }
}