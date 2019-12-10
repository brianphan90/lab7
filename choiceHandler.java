import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Scanner;

public class choiceHandler {


    public void handle(int choice){
        switch (choice){
            case 1:
                break;
            case 2:
                break;
            case 3:
                break;
            case 4:
                deleteRes();
                break;
            case 5:
                break;
            case 6:
                break;

        }

    }
    public void deleteRes() {
        try (Connection conn = DriverManager.getConnection(System.getenv("LAB7_JDBC_URL"),
                System.getenv("LAB7_JDBC_USER"), System.getenv("LAB7_JDBC_PW"))) {
            Scanner input = new Scanner(System.in);
            System.out.println("Please enter in valid reservation code to cancel: ");
            String code = input.nextLine().trim();
            System.out.println("Are you sure you want to cancel (Y or N)");
            String confirmation = input.nextLine().trim();
            if (confirmation.equalsIgnoreCase("y")) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM lab7_reservations WHERE CODE = ?")) {
                    ps.setObject(1, code);
                    int rowCount = ps.executeUpdate();
                    if(rowCount > 0){
                        System.out.println("Deleted " + rowCount + "reservations with code:" + code);
                    }
                    else {
                        System.out.println("No reservations found with code:" + code);
                    }
                }

            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Unable to connect to database");
            System.exit(1);
        }
    }
}