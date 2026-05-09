package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://turntable.proxy.rlwy.net:31353/railway?useSSL=false&serverTimezone=UTC";

    private static final String USER = "root";

    private static final String PASSWORD = "NlwzAxNjSpSuVqcpchvKIgUblDiNkdYE";

    public static Connection getConnection() {
        try {
            return DriverManager.getConnection(
                    URL,
                    USER,
                    PASSWORD
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}