package database;

import java.sql.Connection;
import java.sql.DriverManager;

public class DBConnection {

    private static final String URL =
            "jdbc:mysql://localhost:3306/auction_app";

    private static final String USER = "root";

    private static final String PASSWORD = "123456";

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