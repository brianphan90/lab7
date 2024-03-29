import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;
import java.util.ArrayList;
import java.sql.ResultSet;
import java.sql.Date;
import java.time.LocalDate; 
import java.time.Period; 
public class ChoiceHandler{

    public void handle(int choice) throws SQLException {
        switch (choice){
            case 1:
                roomsAndRates();
                break;
            case 2:
                getReservations();
                break;
            case 3:
                updateRes();
                break;
            case 4:
                deleteRes();
                break;
            case 5:
                detailedRes();
                break;
            case 6:
                showRoomRevenues();
                break;

        }

    }
    public LocalDate convertToLocalDate(Date dateToConvert) {
        return new java.sql.Date(dateToConvert.getTime()).toLocalDate();
    }
    public void printResultSet(ResultSet res) throws SQLException{
      while (res.next()) {
        String rowNum = res.getString("Row_num");
        String roomCode = res.getString("RoomCode");
        String roomName = res.getString("RoomName");
        System.out.println(rowNum + ",  " + roomCode + ", " + roomName);
      }
    }
    public double getTotalCost(double rate, Date checkIn, Date checkOut) {
        LocalDate cIn = convertToLocalDate(checkIn);
        LocalDate cOut = convertToLocalDate(checkOut);
        Period period = Period.between(cOut, cIn);
        int diff = period.getDays();
        double cost = diff * rate * 1.18;
      return cost;
    }
    public String[] printOrderConfirmation(ResultSet res, int rowNum, String firstName, String lastName, Date checkIn, Date checkOut, int numAdults, int numChildren) throws SQLException{
      String[] codeAndRoom= new String[3];
      res.beforeFirst();
      while (res.next()) {
        if (rowNum == res.getInt("Row_num")) {
          String rCode = res.getString("RoomCode");
          String dCode = res.getString("CODE");
          double rate = res.getDouble("Rate");
          String roomName = res.getString("RoomName");
          String dbedType = res.getString("bedType");
          double totalCost = getTotalCost(rate, checkIn, checkOut);
          codeAndRoom[0] = dCode;
          codeAndRoom[1] = rCode;
          codeAndRoom[2] = rate + "";
          // printing order confrimation TODO: calc total cost
          System.out.println("Your order confirmation is printed below:");
          System.out.println("First Name: " + firstName);
          System.out.println("Last Name: " + lastName);
          System.out.println("Room Code: " + rCode);
          System.out.println("Room Name: " + roomName);
          System.out.println("bedType: " + dbedType);
          System.out.println("CheckIn: " + checkIn);
          System.out.println("CheckOut: " + checkOut);
          System.out.println("Number of Adults: " + numAdults);
          System.out.println("Number of Children: " + numChildren);
          System.out.println("Total Cost: " + totalCost);
          break;
        }
      }
      return codeAndRoom;
    }
    public void getReservations() throws SQLException {
    try (Connection conn = DriverManager.getConnection(System.getenv("LAB7_JDBC_URL"),  System.getenv("LAB7_JDBC_USER"), System.getenv("LAB7_JDBC_PW"))) {
        Scanner input = new Scanner(System.in);
        // prompt user input
        System.out.println("Please enter the following values:\nFirst Name:");
        String firstName = input.nextLine().trim();
        System.out.println("Last Name:");
        String lastName = input.nextLine().trim();
        System.out.println("Room Code or 'Any' for no preference:");
        String code = input.nextLine().trim();
        System.out.println("Bed Type or 'Any' for no preference:");
        String bedType = input.nextLine().trim();
        System.out.println("Checkin Date:");
        Date checkIn = Date.valueOf(input.nextLine().trim());
        System.out.println("Checkout Date:");
        Date checkOut = Date.valueOf(input.nextLine().trim());
        System.out.println("Number of Children:");
        int numChildren = Integer.valueOf(input.nextLine().trim());
        System.out.println("Number of Adults:");
        int numAdults = Integer.valueOf(input.nextLine().trim());
        int totalPeople = numAdults + numChildren;
        if (totalPeople > 4) {
          System.out.println("No suitable rooms available");
          return;
        }

        if (code.equals("Any") || bedType.equals("Any")) {
          // if no room preference
          if (code.equals("Any") && !bedType.equals("Any")) {
            try (PreparedStatement d = conn.prepareStatement("select Row_Number() over (Order by RoomCode) as Row_num, RoomCode, RoomName from lab7_rooms ro join lab7_reservations re on RoomCode = Room where (Checkout > ? and checkin < ?) and bedType = ? and maxOcc >= ?")) {
              d.setObject(1, checkIn);
              d.setObject(2, checkOut);
              d.setObject(3, bedType);
              d.setObject(4, totalPeople);
              ResultSet res = d.executeQuery();
              // meaning not an empty set
              if(res.next()) {
                // prints resultset
                printResultSet(res);
                // prompt user to pick choice of which room by roomNumber
                System.out.println("Choose a Row_Num to specify room choice or (Cancel) to return to main menu");
                String rowNum = input.nextLine().trim();
                if (rowNum.equals("Cancel")) {
                  System.out.println("Cancelling Request");
                  return;
                }

                // display user confirmation screen
                int row_Num = Integer.valueOf(rowNum);
                // code is index 0 and roomcode is index 1 and rate is index 2
                String[] codeAndRoom = printOrderConfirmation(res, row_Num, firstName, lastName, checkIn, checkOut, numAdults, numChildren);

                //allowing user to cancel or confirm 
                System.out.println("Please type Cancel or Confirm");
                String confirmation = input.nextLine().trim();
                if (confirmation.equals("Cancel")) {
                  System.out.println("Cancelling Request");
                  return;
                }
                if (confirmation.equals("Confirm")) {
                  // insert new reservation;
                  try (PreparedStatement c = conn.prepareStatement("INSERT INTO lab7_reservations (CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (?,?,?,?,?,?,?,?,?)")) {
                    c.setObject(1, codeAndRoom[0]);
                    c.setObject(2, codeAndRoom[1]);
                    c.setObject(3, checkIn);
                    c.setObject(4, checkOut);
                    c.setObject(5, codeAndRoom[2]);
                    c.setObject(6, lastName);
                    c.setObject(7, firstName);
                    c.setObject(8, numAdults);
                    c.setObject(9, numChildren);
                    c.executeQuery();
                  }
                }
              } else {
                // empty set
                System.out.println("No rooms available. Here are some reccomendations:");
                getReccomendations(checkIn, checkOut, totalPeople, conn);
              }
            }
          }
          // if no bed preference
          if (bedType.equals("Any") && !code.equals("Any")) {
            try (PreparedStatement d = conn.prepareStatement("select Row_Number() over (Order by RoomCode) as Row_num, RoomCode, RoomName from lab7_rooms ro join lab7_reservations re on RoomCode = Room where (Checkout > ? and checkin < ?) and room = (select room from lab7_reservations where code = ?) and maxOcc >= ?")) {
              d.setObject(1,checkIn);
              d.setObject(2,checkOut);
              d.setObject(3,code);
              d.setObject(4,totalPeople);
              ResultSet res = d.executeQuery();
              //check if rooms are available

              if (res.next()) {
                printResultSet(res);
                // prompts user to select room option
                System.out.println("Choose a Row_Num to specify room choice or (Cancel) to return to main menu");
                String rowNum = input.nextLine().trim();
                if (rowNum.equals("Cancel")) {
                  System.out.println("Cancelling Request");
                  return;
                }
                // display user confirmation screen
                int row_Num = Integer.valueOf(rowNum);
                String[] codeAndRoom = printOrderConfirmation(res, row_Num, firstName, lastName, checkIn, checkOut, numAdults, numChildren);

                // checking order confirmation
                //allowing user to cancel or confirm 
                System.out.println("Please type Cancel or Confirm");
                String confirmation = input.nextLine().trim();
                if (confirmation.equals("Cancel")) {
                  System.out.println("Cancelling Request");
                  return;
                }
                if (confirmation.equals("Confirm")) {
                  // insert new reservation;
                  try (PreparedStatement c = conn.prepareStatement("INSERT INTO lab7_reservations (CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (?,?,?,?,?,?,?,?,?)")) {
                    c.setObject(1, codeAndRoom[0]);
                    c.setObject(2, codeAndRoom[1]);
                    c.setObject(3, checkIn);
                    c.setObject(4, checkOut);
                    c.setObject(5, codeAndRoom[2]);
                    c.setObject(6, lastName);
                    c.setObject(7, firstName);
                    c.setObject(8, numAdults);
                    c.setObject(9, numChildren);
                    c.executeQuery();
                  }
                }
              } else {
                // empty set
                System.out.println("No rooms available. Here are some reccomendations:");
                getReccomendations(checkIn, checkOut, totalPeople, conn);                
              }
            }
          }
          // if no bed and room preference
          if (bedType.equals("Any") && code.equals("Any")) {
            try (PreparedStatement d = conn.prepareStatement("select Row_Number() over (Order by RoomCode) as Row_num, RoomCode, RoomName from lab7_rooms ro join lab7_reservations re on RoomCode = Room where (Checkout > ? and checkin < ?) and maxOcc >= ?")) {
              d.setObject(1,checkIn);
              d.setObject(2,checkOut);
              d.setObject(3,totalPeople);
              ResultSet res = d.executeQuery();
              // check if rooms are available
              // if yes
              if (res.next()) {
                // print result set
                printResultSet(res);
                // prompts user to select room option
                System.out.println("Choose a Row_Num to specify room choice or (Cancel) to return to main menu");
                String rowNum = input.nextLine().trim();
                if (rowNum.equals("Cancel")) {
                  System.out.println("Cancelling Request");
                  return;
                }
                // display user confirmation screen
                int row_Num = Integer.valueOf(rowNum);
                String[] codeAndRoom = printOrderConfirmation(res, row_Num, firstName, lastName, checkIn, checkOut, numAdults, numChildren);
                // checking order confirmation
                //allowing user to cancel or confirm 
                System.out.println("Please type Cancel or Confirm");
                String confirmation = input.nextLine().trim();
                if (confirmation.equals("Cancel")) {
                  System.out.println("Cancelling Request");
                  return;
                }
                if (confirmation.equals("Confirm")) {
                  // insert new reservation;
                  try (PreparedStatement c = conn.prepareStatement("INSERT INTO lab7_reservations (CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (?,?,?,?,?,?,?,?,?)")) {
                    c.setObject(1, codeAndRoom[0]);
                    c.setObject(2, codeAndRoom[1]);
                    c.setObject(3, checkIn);
                    c.setObject(4, checkOut);
                    c.setObject(5, codeAndRoom[2]);
                    c.setObject(6, lastName);
                    c.setObject(7, firstName);
                    c.setObject(8, numAdults);
                    c.setObject(9, numChildren);
                    c.executeQuery();
                  }
                }
              } else {
                // empty set
                System.out.println("No rooms available. Here are some reccomendations:");
                getReccomendations(checkIn, checkOut, totalPeople, conn);
              }
            }
          } 
          // if preferences are selected
        } else {
          try (PreparedStatement  d = conn.prepareStatement("select Row_Number() over (Order by RoomCode) as Row_num, RoomCode, RoomName from lab7_rooms ro join lab7_reservations re on RoomCode = Room where (Checkout > ? and checkin < ?) and room = (select room from lab7_reservations where code = ?) and bedType = ? and maxOcc >= ?")) {
            d.setObject(1, checkIn);
            d.setObject(2, checkOut);
            d.setObject(3, code);
            d.setObject(4, bedType);
            d.setObject(5, totalPeople);
            // check if room options are available
            ResultSet res = d.executeQuery();
            // rooms exist
            if (res.next()) {
              //print result set
              printResultSet(res);
              // prompts user to select room option
              System.out.println("Choose a Row_Num to specify room choice or (Cancel) to return to main menu");
              String rowNum = input.nextLine().trim();
              if (rowNum.equals("Cancel")) {
                System.out.println("Cancelling Request");
                return;
              }
              // display user confirmation screen
              int row_Num = Integer.valueOf(rowNum);
              String[] codeAndRoom = printOrderConfirmation(res, row_Num, firstName, lastName, checkIn, checkOut, numAdults, numChildren);
              // checking order confirmation
              //allowing user to cancel or confirm 
              System.out.println("Please type Cancel or Confirm");
              String confirmation = input.nextLine().trim();
              if (confirmation.equals("Cancel")) {
                System.out.println("Cancelling Request");
                return;
              }
              if (confirmation.equals("Confirm")) {
                // insert new reservation;
                try (PreparedStatement c = conn.prepareStatement("INSERT INTO lab7_reservations (CODE, Room, CheckIn, Checkout, Rate, LastName, FirstName, Adults, Kids) VALUES (?,?,?,?,?,?,?,?,?)")) {
                  c.setObject(1, codeAndRoom[0]);
                  c.setObject(2, codeAndRoom[1]);
                  c.setObject(3, checkIn);
                  c.setObject(4, checkOut);
                  c.setObject(5, codeAndRoom[2]);
                  c.setObject(6, lastName);
                  c.setObject(7, firstName);
                  c.setObject(8, numAdults);
                  c.setObject(9, numChildren);
                  c.executeQuery();
                }
              }
            } else {
              // empty set
              System.out.println("No rooms available. Here are some reccomendations:");
              getReccomendations(checkIn, checkOut, totalPeople, conn);
            }
          }
        }
      }
    }
    // TODO
    public void getReccomendations (Date checkIn, Date checkOut, int totalPeople, Connection conn) throws SQLException {
        System.out.println("Reccomended Rooms:");
        try (PreparedStatement  d = conn.prepareStatement("select Row_Number() over (Order by RoomCode) as Row_num, RoomCode, RoomName from lab7_rooms ro join lab7_reservations re on RoomCode = Room where (Checkout > ? and checkin < ?) and maxOcc >= ?")) {
            d.setObject(1, checkIn);
            d.setObject(2, checkOut);
            d.setObject(3, totalPeople);
            ResultSet res = d.executeQuery();
            int i = 0;
            // prints first 5 rooms
            while (res.next() && i < 5) {
                String rowNum = res.getString("Row_num");
                String roomCode = res.getString("RoomCode");
                String roomName = res.getString("RoomName");
                System.out.println(rowNum + ",  " + roomCode + ", " + roomName);
                i++;
            }
        }
    }

    public static void roomsAndRates() throws SQLException{
        try (Connection conn = DriverManager.getConnection
            (System.getenv("LAB7_JDBC_URL"), System.getenv("LAB7_JDBC_USER"), 
            System.getenv("LAB7_JDBC_PW"))) {
            String sql = 
                "with RoomReservations as ( " +
                "select Room, " +
                "( " +
                "case " +
                "when (datediff(curdate(), CheckIn) > 180) " +
                "then (date_add(curdate(), interval -180 day)) " +
                "else CheckIn " +
                "end " +
                ") " +
                "as CheckIn,  " +
                "( " +
                "case " +
                "when (CheckOut > curdate()) " +
                "then (date_add(curdate(), interval 1 day)) " +
                "else CheckOut " +
                "end " +
                ") " +
                "as CheckOut " +
                "from lab7_reservations " +
                "where (curdate() >= CheckIn " +
                "or curdate() > CheckOut) " +
                "and " +
                "(datediff(curdate(), CheckIn) <= 180 " +
                "or datediff(curdate(), CheckOut) <= 180) " +
                "order by Room " +
                "), PopularityScores as ( " +
                "select Room,  " +
                "round(sum(datediff(CheckOut, CheckIn)) / 180, 2) " +
                "as PopularityScore " +
                "from RoomReservations " +
                "group by Room " +
                "order by PopularityScore " +
                "), PastReservations as ( " +
                "select Room, CheckIn, CheckOut " +
                "from lab7_reservations " +
                "where CheckOut <= curdate() " +
                "), MostRecentReservation as ( " +
                "select Room, CheckOut as MostRecentCheckOut, " +
                "datediff(CheckOut, CheckIn) as MostRecentLength " +
                "from PastReservations " +
                "where (Room, CheckOut) in " +
                "( " +
                "select Room, max(CheckOut) " +
                "from PastReservations " +
                "group by Room " +
                ") " +
                "), FutureReservations as ( " +
                "select Room, CheckIn, CheckOut " +
                "from lab7_reservations " +
                "where CheckOut > curdate() " +
                "), AvailableCheckInDates as ( " +
                "select Room, CheckOut as PossibleCheckIn " +
                "from FutureReservations " +
                "where (Room, CheckOut) not in ( " +
                "select Room, CheckIn " +
                "from FutureReservations " +
                ") " +
                "), NextAvailableCheckIn as ( " +
                "select Room, min(PossibleCheckIn)  " +
                "as NextAvailableDate " +
                "from AvailableCheckInDates " +
                "group by Room " +
                ") " +
                "select mr.Room, PopularityScore, " +
                "NextAvailableDate, " +
                "MostRecentLength, " +
                "MostRecentCheckOut " +
                "from PopularityScores ps " +
                "join MostRecentReservation mr " +
                "on ps.Room = mr.Room " +
                "join NextAvailableCheckIn nci " +
                "on nci.Room = mr.Room " +
                "order by PopularityScore desc; ";

            try(Statement stmt = conn.createStatement()){

              try(ResultSet rs = stmt.executeQuery(sql)){


                System.out.format("|%4s|%15s|%17s|%18s|%18s|\n", 
                  "Room", "PopularityScore", "NextAvailableDate","MostRecentLength","MostRecentCheckOut");

                while (rs.next()) {
                    System.out.format("|%4s|%15.2f|%17s|%18d|%18s|\n",
                            rs.getString("Room"), rs.getDouble("PopularityScore"), 
                            rs.getDate("NextAvailableDate").toString(), rs.getInt("MostRecentLength"),
                            rs.getDate("MostRecentCheckOut").toString());

                }
              }
            }

        }
    }

   public static void updateRes() {
      try (Connection conn = DriverManager.getConnection(System.getenv("LAB7_JDBC_URL"), System.getenv("LAB7_JDBC_USER"), System.getenv("LAB7_JDBC_PW"))) {
         Scanner input = new Scanner(System.in);
         System.out.println("Please enter in valid reservation code to change: ");
         String code = input.nextLine().trim();
         Date cIn, cOut;
         ResultSet res;
         try(PreparedStatement s = conn.prepareStatement("Select Checkin, Checkout from lab7_reservations where Code = ?")){
            s.setObject(1, code);
            res = s.executeQuery();
            if(res.first()){
               cIn = res.getDate("Checkin");
               cOut = res.getDate("Checkout");
            }
            else{
               System.out.println("No reservations found with code: " + code);
               return;
            }
         }
         System.out.println("Please enter the following values(- to keep existing value):\nFirst Name:");
         String firstName = input.nextLine().trim();
         System.out.println("Last Name:");
         String lastName = input.nextLine().trim();
         System.out.println("Checkin Date:");
         String checkIn = input.nextLine().trim();
         System.out.println("Checkout Date:");
         String checkOut = input.nextLine().trim();
         System.out.println("Number of Children:");
         String children = input.nextLine().trim();
         System.out.println("Number of Adults:");
         String adults = input.nextLine().trim();
         String stmt = "UPDATE lab7_reservations SET ";
         int i = 1;
         int pos[] = {0,0,0,0,0,0,0};
         if(!firstName.equals("-")){
            stmt = stmt.concat("FirstName = ? ,");
            pos[0] = i++;
         }
         if(!lastName.equals("-")){
            stmt = stmt.concat("LastName = ? ,");
            pos[1] = i++;
         }
         if(!children.equals("-")){
            stmt = stmt.concat("Kids = ? ,");
            pos[2] = i++;
         }
         if(!adults.equals("-")){
            stmt = stmt.concat("Adults = ? ,");
            pos[3] = i++;
         }
         if(!checkIn.equals("-")){
            cIn = Date.valueOf(checkIn);
         }
         if(!checkOut.equals("-")){
            cOut = Date.valueOf(checkOut);
         }
         try(PreparedStatement d = conn.prepareStatement("select * from lab7_reservations where (Checkout > ? and checkin < ?) and room = (select room from lab7_reservations where code = ?) and code != ?;")){
            d.setObject(1, cIn);
            d.setObject(2, cOut);
            d.setObject(3, code);
            d.setObject(4, code);
            ResultSet count = d.executeQuery();
            if(count.next()){
               pos[4] = 0;
               pos[5] = 0;
               System.out.println("New dates conflict with existing reservations");
            }
            else{
               stmt = stmt.concat("Checkin = ? ,");
               pos[4] = i++;
               stmt = stmt.concat("Checkout = ? ,");
               pos[5] = i++;
            }
         }
         pos[6] = i;
         stmt = stmt.substring(0, stmt.length()-1);
         stmt = stmt.concat("where code = ?;");
         try(PreparedStatement ps = conn.prepareStatement(stmt)) {
            if(pos[0] != 0)
               ps.setObject(pos[0], firstName);
            if(pos[1] != 0)
               ps.setObject(pos[1], lastName);
            if(pos[2] != 0)
               ps.setObject(pos[2], Integer.parseInt(children));
            if(pos[3] != 0)
               ps.setObject(pos[3], Integer.parseInt(adults));
            if(pos[4] != 0)
               ps.setDate(pos[4], cIn);
            if(pos[5] != 0)
               ps.setDate(pos[5], cOut);
            ps.setObject(pos[6], Integer.parseInt(code));

            if(pos[6] != 1){
               int rowCount = ps.executeUpdate();
               if(rowCount > 0){
                  System.out.println("Updated reservation with code: " + code);
               }
               else {
                  System.out.println("No reservations found with code: " + code);
               }
            }else{
               System.out.println("No new information entered");
            }
            conn.close();
         }
      }catch (Exception e) {
         System.err.println(e.getMessage());
         System.err.println("Unable to connect to database");
         System.exit(1);
     }
   }
    public static void deleteRes() {
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
    public static void detailedRes(){
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


    public static void showRoomRevenues() {
        try (Connection conn = DriverManager.getConnection(System.getenv("LAB7_JDBC_URL"),
                System.getenv("LAB7_JDBC_USER"), System.getenv("LAB7_JDBC_PW"))) {
                try (PreparedStatement pstmt = conn.prepareStatement("WITH mainTable AS(\n" +
                        "SELECT Roomname, Month, SUM(Revenue) AS Revenue\n" +
                        "    FROM(\n" +
                        "        SELECT RoomName,MONTHNAME(newCheckIn) AS Month, DATEDIFF(newCheckout, newCheckIn) * Rate AS Revenue\n" +
                        "        FROM(\n" +
                        "            SELECT \n" +
                        "            RoomName,\n" +
                        "            Rate,\n" +
                        "            CheckIn AS newCheckIn, \n" +
                        "            CASE WHEN MONTH(CheckIn) != MONTH (Checkout) THEN LAST_DAY(CheckIn) ELSE Checkout END AS newCheckout\n" +
                        "            FROM lab7_reservations res\n" +
                        "            JOIN lab7_rooms r\n" +
                        "            ON res.Room = r.RoomCode\n" +
                        "        ) AS a\n" +
                        "        \n" +
                        "        UNION\n" +
                        "        \n" +
                        "        SELECT RoomName, MONTHNAME(newCheckIn) AS Month, DATEDIFF(newCheckout, newCheckIn) * Rate AS Revenue\n" +
                        "        FROM(\n" +
                        "            SELECT \n" +
                        "            RoomName,\n" +
                        "            Rate,\n" +
                        "            CASE WHEN MONTH(CheckIn) != MONTH (Checkout) THEN DATE_ADD(LAST_DAY(CheckIn), INTERVAL 1 DAY) END AS newCheckIn,\n" +
                        "            Checkout AS newCheckout\n" +
                        "            FROM lab7_reservations res\n" +
                        "            JOIN lab7_rooms r\n" +
                        "            ON res.Room = r.RoomCode\n" +
                        "        ) AS b\n" +
                        "        WHERE b.newCheckIn IS NOT NULL\n" +
                        "    )AS main\n" +
                        "    GROUP BY RoomName, Month\n" +
                        ")\n" +
                        "SELECT c.RoomName,\n" +
                        "SUM(January) AS January,\n" +
                        "SUM(February) AS February,\n" +
                        "SUM(March) AS March,\n" +
                        "SUM(April) AS April,\n" +
                        "SUM(May) AS May,\n" +
                        "SUM(June) AS June,\n" +
                        "SUM(July) AS July,\n" +
                        "SUM(August) AS August,\n" +
                        "SUM(September) AS September,\n" +
                        "SUM(October) AS October,\n" +
                        "SUM(November) AS November,\n" +
                        "SUM(December) AS December,\n" +
                        "d.Total\n" +
                        "FROM(\n" +
                        "    select Roomname,\n" +
                        "    CASE WHEN Month = \"January\" THEN Revenue END AS January,\n" +
                        "    CASE WHEN Month = \"February\" THEN Revenue END AS February,\n" +
                        "    CASE WHEN Month = \"March\" THEN Revenue END AS March,\n" +
                        "    CASE WHEN Month = \"April\" THEN Revenue END AS April,\n" +
                        "    CASE WHEN Month = \"May\" THEN Revenue END AS May,\n" +
                        "    CASE WHEN Month = \"June\" THEN Revenue END AS June,\n" +
                        "    CASE WHEN Month = \"July\" THEN Revenue END AS July,\n" +
                        "    CASE WHEN Month = \"August\" THEN Revenue END AS August,\n" +
                        "    CASE WHEN Month = \"September\" THEN Revenue END AS September,\n" +
                        "    CASE WHEN Month = \"October\" THEN Revenue END AS October,\n" +
                        "    CASE WHEN Month = \"November\" THEN Revenue END AS November,\n" +
                        "    CASE WHEN Month = \"December\" THEN Revenue END AS December\n" +
                        "    from mainTable\n" +
                        ")AS c\n" +
                        "\n" +
                        "JOIN(\n" +
                        "    SELECT Roomname, SUM(Revenue) AS Total\n" +
                        "        FROM(\n" +
                        "            SELECT RoomName,MONTHNAME(newCheckIn) AS Month, DATEDIFF(newCheckout, newCheckIn) * Rate AS Revenue\n" +
                        "            FROM(\n" +
                        "                SELECT \n" +
                        "                RoomName,\n" +
                        "                Rate,\n" +
                        "                CheckIn AS newCheckIn, \n" +
                        "                CASE WHEN MONTH(CheckIn) != MONTH (Checkout) THEN LAST_DAY(CheckIn) ELSE Checkout END AS newCheckout\n" +
                        "                FROM lab7_reservations res\n" +
                        "                JOIN lab7_rooms r\n" +
                        "                ON res.Room = r.RoomCode\n" +
                        "            ) AS a\n" +
                        "            \n" +
                        "            UNION\n" +
                        "            \n" +
                        "            SELECT RoomName, MONTHNAME(newCheckIn) AS Month, DATEDIFF(newCheckout, newCheckIn) * Rate AS Revenue\n" +
                        "            FROM(\n" +
                        "                SELECT \n" +
                        "                RoomName,\n" +
                        "                Rate,\n" +
                        "                CASE WHEN MONTH(CheckIn) != MONTH (Checkout) THEN DATE_ADD(LAST_DAY(CheckIn), INTERVAL 1 DAY) END AS newCheckIn,\n" +
                        "                Checkout AS newCheckout\n" +
                        "                FROM lab7_reservations res\n" +
                        "                JOIN lab7_rooms r\n" +
                        "                ON res.Room = r.RoomCode\n" +
                        "            ) AS b\n" +
                        "            WHERE b.newCheckIn IS NOT NULL\n" +
                        "        )AS main\n" +
                        "        GROUP BY RoomName\n" +
                        ") AS d\n" +
                        "ON c.RoomName = d.Roomname\n" +
                        "GROUP BY c.Roomname\n"))
                {
                    try (ResultSet rs = pstmt.executeQuery()){
                        int count = 0;
                        System.out.format("%30s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s\n", "RoomName", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December", "Total");
                        while (rs.next()) {
                            System.out.format("%30s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s |%10s\n",
                                    rs.getString("RoomName"), String.format("$%.2f", rs.getDouble("January")), String.format("$%.2f", rs.getDouble("February")), String.format("$%.2f", rs.getDouble("March")
                                    ), String.format("$%.2f", rs.getDouble("April")), String.format("$%.2f", rs.getDouble("May")), String.format("$%.2f", rs.getDouble("June")), String.format("$%.2f", rs.getDouble("July")
                                    ), String.format("$%.2f", rs.getDouble("August")), String.format("$%.2f", rs.getDouble("September")), String.format("$%.2f", rs.getDouble("October")), String.format("$%.2f", rs.getDouble("November")
                                    ), String.format("$%.2f", rs.getDouble("December")), String.format("$%.2f", rs.getDouble("Total")));
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
