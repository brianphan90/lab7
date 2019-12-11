import java.sql.Connection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Scanner;

public class connection {
    public connection() {
    }

   /* public static void getConnection() {
        try {
            Connection var0 = DriverManager.getConnection(System.getenv("LAB7_JDBC_URL"), System.getenv("LAB7_JDBC_USER"), System.getenv("LAB7_JDBC_PW"));
        } catch (SQLException var1) {
            System.err.println(var1.getMessage());
            System.err.println("Unable to connect to database");
            System.exit(1);
        }

        System.out.println("Established database connection");
    }*/

    public static void showOptions(){
        choiceHandler handler = new choiceHandler();
        String prompt = "(0) Quit\n" +
                "(1) Rooms and Rates\n" +
                "(2) Reservations\n" +
                "(3) Reservation Change\n" +
                "(4) Reservation Cancellation\n" +
                "(5) Detailed Reservation Information\n" +
                "(6) Revenue\n";
        System.out.print(prompt);
        System.out.print("Enter input: ");
        Scanner input = new Scanner(System.in);
        int choice = input.nextInt();
        while(choice != 0){
            System.out.println(choice);
            handler.handle(choice);
            System.out.print("Enter input: ");
            choice = input.nextInt();
        }
    }

    public static void main(String[] var0) {
        showOptions();

    }
}