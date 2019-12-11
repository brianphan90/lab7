import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Scanner;
import java.util.ArrayList;
import java.sql.ResultSet;
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
                detailedRes();
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
                        System.out.println("Deleted " + rowCount + "reservations with code: " + code);
                    }
                    else {
                        System.out.println("No reservations found with code: " + code);
                    }
                }

            }
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Unable to connect to database");
            System.exit(1);
        }
    }
    public void detailedRes(){
        try (Connection conn = DriverManager.getConnection(System.getenv("LAB7_JDBC_URL"),
                System.getenv("LAB7_JDBC_USER"), System.getenv("LAB7_JDBC_PW"))) {
            Scanner input = new Scanner(System.in);
            int prevExist = 0;
            ArrayList<Object> params = new ArrayList<>();
            StringBuilder sb = new StringBuilder("SELECT * FROM lab7_reservations res join lab7_rooms r on res.Room = r.RoomCode ");
            System.out.println("Enter a valid first name: ");
            String fName = input.nextLine().trim();
            if(fName.length() > 0){
                fName += '%';
                sb.append("WHERE FirstName like ? ");
                params.add(fName);
                prevExist = 1;
            }
            System.out.println("Enter a valid LastName: ");
            String lName = input.nextLine().trim();
            if(lName.length() > 0){
                lName += '%';
                if(prevExist > 0){
                    sb.append("AND LastName like ? ");
                }
                else{
                    sb.append("WHERE LastName like ?");
                    prevExist = 1;
                }
                params.add(lName);
            }
            System.out.println("Enter a valid CheckIn date: ");
            String startDate = input.nextLine().trim();
            if(startDate.length() > 0){
                if(prevExist > 0){
                    sb.append("AND CheckIn >= ? ");
                }
                else{
                    sb.append("WHERE CheckIn >= ? ");
                    prevExist = 1;
                }
                params.add(startDate);
            }
            System.out.println("Enter a valid CheckOut date");
            String endDate = input.nextLine().trim();
            if(endDate.length() > 0){
                if(prevExist > 0){ 
                    sb.append("AND CheckOut <= ? ");
                }
                else {
                    sb.append("WHERE CheckOut <= ?");
                    prevExist = 1;
                }
                params.add(endDate);
            }
            System.out.println("Enter a valid Room Code: ");
            String roomCode = input.nextLine().trim();
            if(roomCode.length() > 0){
                roomCode = roomCode + '%';
                if(prevExist > 0){
                    sb.append("AND Room like ? ");
                }
                else {
                    sb.append("WHERE Room like ? ");
                    prevExist = 1;
                }
                params.add(roomCode);
            }
            System.out.println("Enter a Reservation Code: ");
            String resCode = input.nextLine().trim();
            if(resCode.length() > 0){
                resCode += '%';
                if(prevExist > 0){
                    sb.append("AND CODE like ?");

                }
                else {
                    sb.append("WHERE CODE like ?");
                }
                params.add(resCode);
            }
            try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())){
                int i = 1;
                for(Object p : params){
                    pstmt.setObject(i++,p);
                }

                try (ResultSet rs = pstmt.executeQuery()){
                    int count = 0;
                    System.out.format("%6s |%4s |%10s |%10s |%6.2s |%20s |%20s |%6s |%4s |%s\n", "CODE", "Room", "Checkin", "Checkout", "Rate", "LastName", "FirstName", "Adults", "Kids", "RoomName");
                    while (rs.next()) {
                        System.out.format("%6s |%4s |%10s |%10s |%6.2f |%20s |%20s |%6d |%4d |%30s\n",
                                rs.getString("CODE"), rs.getString("room"), rs.getDate("checkin").toString(),
                                rs.getDate("checkout").toString(), rs.getDouble("rate"), rs.getString("lastname"),
                                rs.getString("firstname"), rs.getInt("adults"), rs.getInt("kids"), rs.getString("roomname"));
                        count++;
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
