package cs320.model;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by Walter Schaertl on 3/23/2019.
 */
public class CreateNewDatabase {

    // Happens only once to populate the database from the csvs
    public void packageEmployees() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS package_employee("
                + "ID INT PRIMARY KEY,"
                + "USERNAME VARCHAR(255),"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./H2Demo/CSVs/package_employees.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("employeeID")) {
                    query = String.format("INSERT INTO package_employee VALUES(%s,\'%s\');", split[0], split[1]);
                    conn.createStatement().execute(query);
                    conn.createStatement().execute("CREATE USER " + split[1] + " PASSWORD 'password';");
                    // Package users have all abilities on transaction table (maybe change?)
                    conn.createStatement().execute("GRANT ALL ON TRANSACTION TO " + split[1]);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void transaction() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS transaction("
                + "ID INT PRIMARY KEY auto_increment,"
                + "DATE DATE,"
                + "TIME TIME,"
                + "EMPLOYEE_ID_FK INT,"
                + "LOCATION_ID_FK VARCHAR(255),"
                + "ACCOUNT_NUMBER_FK INT,"
                + "PACKAGE_SERIAL_FK VARCHAR(255),"
                + ");";
        Statement stmt = conn.createStatement();
        stmt.execute(query);
    }

    public void accountingEmployees() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS accounting_employee("
                + "ID INT PRIMARY KEY,"
                + "USERNAME VARCHAR(255),"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./H2Demo/CSVs/accounting_employees.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("employeeID")) {
                    query = String.format("INSERT INTO accounting_employee VALUES(%s,\'%s\');", split[0], split[1]);
                    conn.createStatement().execute(query);
                    try{conn.createStatement().execute("CREATE USER " + split[1] + " PASSWORD 'password';");}
                    catch(SQLException e){;}
                    // This is probably too many permissions
                    conn.createStatement().execute("GRANT ALL ON CUSTOMER, BILLING, TRANSACTION, PACKAGE TO " + split[1]);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void packageInit() throws SQLException{
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS package("
                + "ACCOUNT_NUMBER_FK INT,"
                + "SERVICE_ID_FK INT,"
                + "SERIAL VARCHAR(255),"
                + "HEIGHT DOUBLE,"
                + "LENGTH DOUBLE,"
                + "DEPTH DOUBLE,"
                + "WEIGHT DOUBLE,"
                + "SIGNED_FOR_BY VARCHAR(255),"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./H2Demo/CSVs/package.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("account_number_fk")) {
                    query = String.format("INSERT INTO package VALUES(%s,%s,'%s',%s,%s,%s,%s,'%s');",
                            split[0], split[1], split[2], split[3], split[4], split[5], split[6], split[7]);
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void customers() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS customer("
                + "ACCOUNT_NUMBER INT PRIMARY KEY auto_increment,"
                + "BILLING_FK INT,"
                + "NEGOTIATED_RATE_ID_FK INT,"
                + "MAILING_ADDRESS_ID_FK INT,"
                + "FIRST_NAME VARCHAR(255),"
                + "LAST_NAME VARCHAR(255),"
                + "PHONE_NO VARCHAR(255),"
                + "USERNAME VARCHAR(255)," // Can be null
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./H2Demo/CSVs/customer.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("account_number")) {
                    query = String.format("INSERT INTO customer VALUES(%s,%s,%s,%s,'%s','%s','%s','%s');",
                            split[0], split[1], split[2], split[3], split[4], split[5], split[6], split[7]);
                    conn.createStatement().execute(query);
                    conn.createStatement().execute("CREATE USER " + split[7] + " PASSWORD 'password';");
                    // Package users have all abilities on transaction table (maybe change?)
                    //conn.createStatement().execute("GRANT ALL ON package TO " + split[1]);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void billing() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS billing("
                + "ID INT PRIMARY KEY auto_increment,"
                + "BALANCE_TO_DATE DOUBLE,"
                + "PAY_MODEL VARCHAR(255),"
                + "ACCOUNT_NUMBER_FK INT,"
                + "EMPLOYEE_ID INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./H2Demo/CSVs/billing.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("ID")) {
                    query = String.format("INSERT INTO billing VALUES(%d, %f,'%s',%d,%d);",
                            Integer.parseInt(split[0].replace("\"","")),
                            Double.parseDouble(split[1].replace("\"","")), split[2],
                            Integer.parseInt(split[3].replace("\"","")),
                            Integer.parseInt(split[4].replace("\"","")));
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
