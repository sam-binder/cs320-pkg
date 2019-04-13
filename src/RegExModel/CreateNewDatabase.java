package RegExModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * Created by Walter Schaertl on 3/23/2019.
 */

/**
 * This class is used to spin up a new clean database using the correlated
 * provided CSV files. Each table can be generated individually if it gets
 * corrupted or dropped and needs to be readded. NOTE: this should not
 * be run on existing tables, it will more than likely fair with primary
 * key conflicts.
 * @author Walter Schaertl
 * @date 3/24/19
 */
public class CreateNewDatabase {

    /**
     * Public constructor to set up a the first database connection.
     *  Uses the username password pair of "me", "password".
     */
    public CreateNewDatabase(){
        H2Access h2 = new H2Access();
        try {
            Connection c = h2.createConnection("me", "password");
            h2.closeConnection(c);
        } catch(SQLException sqle) {
            sqle.printStackTrace();
        }
    }

    /**
     * Sets up the package_employee table from the package_employees.csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void packageEmployees() throws SQLException {
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS package_employee("
                + "ID INT PRIMARY KEY,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/package_employees.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("employeeID")) {
                    query = String.format("INSERT INTO package_employee VALUES(%s);", split[0]);
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        conn.close();
    }

    /**
     * Sets up the transaction table, empty by default as nothing has happened yet
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void transaction() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS transaction("
                // Autoincrement for easy of adding future transactions
                + "ID INT PRIMARY KEY auto_increment,"
                + "DATE DATE,"
                + "TIME TIME,"
                + "EMPLOYEE_ID_FK INT,"
                + "LOCATION_ID_FK CHAR(12),"
                + "ACCOUNT_NUMBER_FK INT,"
                + "PACKAGE_SERIAL_FK CHAR(6),"
                + ");";
        Statement stmt = conn.createStatement();
        stmt.execute(query);
    }

    /**
     * Sets up the accounting_employee table from the accounting_employees csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void accountingEmployees() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS accounting_employee("
                + "ID INT PRIMARY KEY,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/accounting_employees.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("employeeID")) {
                    query = String.format("INSERT INTO accounting_employee VALUES(%s);", split[0]);
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the package table from the package csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void packageInit() throws SQLException{
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS package("
                + "ACCOUNT_NUMBER_FK INT,"
                + "SERVICE_ID_FK INT,"
                + "SERIAL CHAR(6),"
                + "HEIGHT INT,"
                + "LENGTH INT,"
                + "DEPTH INT,"
                + "WEIGHT INT,"
                + "SIGNED_FOR_BY VARCHAR(255),"
                + "ORIGIN_FK CHAR(12),"
                + "DESTINATION_FK CHAR(12)"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/package.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("account_number_fk")) {
                    query = String.format("INSERT INTO package VALUES(%s,%s,'%s',%s,%s,%s,%s,'%s','%s','%s');",
                            split[0], split[1], split[2], split[3], split[4], split[5], split[6], split[7], "null", "null");
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the customer table from the customer csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void customers() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS customer("
                // Autoincrement for easy of adding future customers
                + "ACCOUNT_NUMBER INT PRIMARY KEY auto_increment,"
                + "BILLING_FK INT,"
                + "NEGOTIATED_RATE_ID_FK INT,"
                + "MAILING_ADDRESS_ID_FK INT,"
                + "FIRST_NAME VARCHAR(255),"
                + "LAST_NAME VARCHAR(255),"
                + "PHONE_NO VARCHAR(255),"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/customer.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("account_number")) {
                    query = String.format("INSERT INTO customer VALUES(%s,%s,%s,%s,'%s','%s','%s');",
                            split[0], split[1], split[2], split[3], split[4], split[5], split[6]);
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the billing table from the billing csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void billing() throws SQLException {
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS billing("
                // Autoincrement for easy of adding future billing
                + "ID INT PRIMARY KEY auto_increment,"
                + "BALANCE_TO_DATE DOUBLE,"
                + "PAY_MODEL VARCHAR(255),"
                + "ACCOUNT_NUMBER_FK INT,"
                + "EMPLOYEE_ID INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/billing.csv"));
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

    /**
     * Sets up the user table from the user csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void users() throws  SQLException{
        H2Access h2 = new H2Access();
        Connection conn = h2.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS user("
                + "GENERAL_FK INT,"
                + "USERNAME VARCHAR(255) PRIMARY KEY,"
                + "PASSWORD VARCHAR(255),"
                + "TYPE VARCHAR(255)"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/user.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("account_number_fk")) {
                    query = String.format("INSERT INTO user VALUES(%s,'%s','%s','%s');",
                            split[0], split[1], split[2], split[3]);
                    conn.createStatement().execute(query);
                    String username = split[1];
                    try {
                        conn.createStatement().execute("CREATE USER " + username + " PASSWORD '" + split[2] + "';");
                    } catch (SQLException e){}
                    if(split[3].equals("customer")) {
                        for(String table: Arrays.asList("PACKAGE", "USER", "ADDRESS", "ZIP_CODE", "CUSTOMER"))
                            conn.createStatement().execute("GRANT ALL ON " + table + " TO " + username);
                    } else if(split[3].equals("accounting_employee")) {
                        for(String table: Arrays.asList("CUSTOMER", "PACKAGE", "BILLING", "TRANSACTION", "USER"))
                            conn.createStatement().execute("GRANT ALL ON " + table + " TO " + username);
                    } else if(split[3].equals("package_employee")){
                        for(String table: Arrays.asList("TRANSACTION", "PACKAGE", "PACKAGE_EMPLOYEE", "USER"))
                            conn.createStatement().execute("GRANT ALL ON " + table + " TO " + username);
                    }
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the address table from the address csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void address() throws SQLException{
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS address("
                // Autoincrement for easy of adding future addresses
                + "ID INT PRIMARY KEY auto_increment,"
                + "COMPANY VARCHAR(255),"
                + "ATTN VARCHAR(255),"
                + "STREET_LINE_1 VARCHAR(255),"
                + "STREET_LINE_2 VARCHAR(255),"
                + "ZIP_FK INT," // TODO char(5)
                + "ACCOUNT_NUMBER_FK INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/address.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("ID")) {
                    query = String.format("INSERT INTO address VALUES(%d, '%s','%s', '%s', '%s', %d, %d);",
                            Integer.parseInt(split[0].replace("\"", "")),
                            split[1].replace("\"", ""), split[2].replace("\"", ""), split[3].replace("\"", ""), split[4].replace("\"", ""),
                            Integer.parseInt(split[5].replace("\"", "")),
                            Integer.parseInt(split[6].replace("\"", "")));
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the charges table from the charges csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void charges() throws SQLException{
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS charge("
                // Autoincrement for easy of adding future charges
                + "ID INT PRIMARY KEY auto_increment,"
                + "PRICE DOUBLE,"
                + "ACCOUNT_NUMBER_FK INT,"
                + "PACKAGE_SERIAL_FK VARCHAR(255),"
                + "SERVICE_ID INT,"
                + "PAID INT"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/charges.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("ID")) {
                    query = String.format("INSERT INTO charge VALUES(%d, %f, %d, '%s', %d, %d);",
                            Integer.parseInt(split[0].replace("\"", "")),
                            Double.parseDouble(split[1].replace("\"", "")),
                            Integer.parseInt(split[2].replace("\"", "")),
                            split[3].replace("\"", ""),
                            Integer.parseInt(split[4].replace("\"", "")), 0);
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the location table from the location csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void location() throws SQLException{
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS location("
                + "ID CHAR(12) PRIMARY KEY,"
                + "ADDRESS_ID INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/location.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("ID")) {
                    if (split.length > 1) {
                        int id = Integer.parseInt(split[1].replace("\"", ""));
                        query = String.format("INSERT INTO location VALUES('%s', %d);", split[0], id);
                    } else
                        query = String.format("INSERT INTO location VALUES('%s', null);", split[0]);
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the priority table from the priority csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void priority() throws SQLException{
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS priority("
                + "ID INT PRIMARY KEY,"
                + "AIR_GROUND INT,"
                + "RUSH INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/priority.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("ID")) {
                    query = String.format("INSERT INTO priority VALUES(%d, %d, %d);",
                            Integer.parseInt(split[0].replace("\"", "")),
                            Integer.parseInt(split[1].replace("\"", "")),
                            Integer.parseInt(split[2].replace("\"", "")));
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the rate table from the rates csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void rates() throws SQLException{
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS rate("
                + "NEGOTIATED_RATE_ID INT PRIMARY KEY,"
                + "GROUND_RATE DOUBLE,"
                + "AIR_RATE DOUBLE,"
                + "RUSH_RATE DOUBLE,"
                + "DIM_RATING_BREAK INT,"
                + "EMPLOYEE_ID INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/rates.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("negotiated_rate_ID")) {
                    query = String.format("INSERT INTO rate VALUES(%d, %f, %f, %f, %d, %d);",
                            Integer.parseInt(split[0].replace("\"", "")),
                            Double.parseDouble(split[1].replace("\"", "")),
                            Double.parseDouble(split[2].replace("\"", "")),
                            Double.parseDouble(split[3].replace("\"", "")),
                            Integer.parseInt(split[4].replace("\"", "")),
                            Integer.parseInt(split[5].replace("\"", "")));
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the service table from the service csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void service() throws SQLException{
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS service("
                + "ID INT PRIMARY KEY,"
                + "PRIORITY_FK INT,"
                + "HAZARDOUS INT,"
                + "SIGNATURE_REQ INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/service.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("ID")) {
                    query = String.format("INSERT INTO service VALUES(%d, %d, %d, %d);",
                            Integer.parseInt(split[0].replace("\"", "")),
                            Integer.parseInt(split[1].replace("\"", "")),
                            Integer.parseInt(split[2].replace("\"", "")),
                            Integer.parseInt(split[3].replace("\"", "")));
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the zip_code table from the zipcodes csv
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    public void zipCodes() throws SQLException{
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS zip_code("
                + "ID INT PRIMARY KEY,"
                + "ZIP_CODE CHAR(5),"
                + "LATITUDE double,"
                + "LONGITUDE double,"
                + "CITY VARCHAR(30),"
                + "STATE CHAR(2),"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/csvs/zipcodes.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("ID")) {
                    query = String.format("INSERT INTO zip_code VALUES(%d, '%s', %f, %f, '%s', '%s');",
                            Integer.parseInt(split[0].replace("\"", "")),
                            split[1].replace("\"", ""),
                            Double.parseDouble(split[2].replace("\"", "")),
                            Double.parseDouble(split[3].replace("\"", "")),
                            split[4].replace("\"", ""),
                            split[5].replace("\"", ""));
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Initializes all the tables.
     */
    public void initDatabase(){
        H2Access h2 = new H2Access();
        try {
            this.transaction();
            this.packageInit();
            this.customers();
            this.billing();
            this.accountingEmployees();
            this.packageEmployees();
            this.address();
            this.charges();
            this.location();
            this.priority();
            this.rates();
            this.service();
            this.zipCodes();
            this.users();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /* THESE DONT EXIST IN H2 - PRETTY NEAT WE GOT TO LEARN ABOUT EM THO
    public void buildFunctions() throws SQLException{
        // Tracking function
        buildTrackingFunction();
        // Accounting Employee Functions:
        //      view customer report

    }


    public void buildTrackingFunction() throws SQLException{
        Connection conn = new H2Access().createConnection("me", "password");
        String query = "CREATE FUNCTION get_tracking(account_number_fk int, package_serial_fk int)"
                + "RETURNS table ("
                + "ID int,"
                + "date date,"
                + "time time,"
                + "location_ID_fk char(12))"
                + "RETURN table"
                + "(SELECT ID, date, time, location_ID_fk"
                + "FROM Transaction"
                + "WHERE Transaction.account_number_fk = get_tracking.account_number_fk"
                + "AND Transaction.package_serial_fk = get_tracking.package_serial_fk)";
        Statement stmt = conn.createStatement();
        stmt.execute(query);
    }
    */


}
