import java.sql.Connection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class connection {
    public connection() {
    }

    public static void getConnection() {
        try {
            System.out.println("we in");
            Connection var0 = DriverManager.getConnection(System.getenv("LAB7_JDBC_URL"), System.getenv("LAB7_JDBC_USER"), System.getenv("LAB7_JDBC_PW"));
        } catch (SQLException var1) {
            System.err.println(var1.getMessage());
            System.err.println("Unable to connect to database");
            System.exit(1);
        }

        System.out.println("Established database connection");
    }

    public static void main(String[] var0) {
        getConnection();
    }
}