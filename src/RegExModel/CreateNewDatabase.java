package RegExModel;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;

/**
 * This class is used to spin up a new clean database using the correlated provided CSV files. Each table can be generated individually if it gets corrupted or dropped and needs to be readded. NOTE: this should not be run on existing tables, it will more than likely fair with primary key conflicts.
 * @author Walter Schaertl
 * @date 3/24/19
 */
public class CreateNewDatabase {
    /**
     * Sets up the package_employee table from the package_employees.csv.
     *
     * @throws SQLException if any number of  things go wrong, from failure to establish a connection to failure to execute a query.
     */
    private static void packageEmployees() throws SQLException {
        // opens a connection
        Connection conn = H2Access.createConnection("me", "password");
        // builds a query to create the table
        String query = "CREATE TABLE IF NOT EXISTS package_employee("
                + "ID INT PRIMARY KEY,"
                + ");" ;
        // creates a statement object with the connection
        conn.createStatement().execute(query);

        // next we have to go through and ingest the CSV associated with this table
        try {
            // creates a new buffered reader pointing to the file
            BufferedReader br = new BufferedReader(
                new FileReader("./src/RegExModel/CSVs/package_employees.csv")
            );

            // skips the first line which is always a "header" line
            br.readLine();

            // will be the line read in
            String line;
            while((line = br.readLine()) != null){
                // split the line by comma
                String[] split = line.split(",");

                // builds a query
                query = String.format("INSERT INTO package_employee VALUES(%s);", split[0]);
                // creates and executes the query to insert the data
                conn.createStatement().execute(query);
            }

            // close the reader
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // close connection to the database
        conn.close();
    }

    /**
     * Sets up the transaction table; will be empty by default as nothing has happened yet.
     *
     * @throws SQLException if any number of  things go wrong, from failure to establish a connection to failure to execute a query.
     */
    private static void transaction() throws SQLException {
        // creates a new connection to the database
        Connection conn = H2Access.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS transaction("
                    + "ID INT PRIMARY KEY auto_increment,"
                    + "DATE DATE,"
                    + "TIME TIME,"
                    + "EMPLOYEE_ID_FK INT,"
                    + "LOCATION_ID_FK CHAR(12),"
                    + "ACCOUNT_NUMBER_FK INT,"
                    + "PACKAGE_SERIAL_FK CHAR(6),"
                + ");";

        // builds and queries the table creation
        conn.createStatement().execute(query);

        // closes connection to the db
        conn.close();
    }

    /**
     * Sets up the accounting_employee table from the accounting_employees CSV.
     *
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void accountingEmployees() throws SQLException {
        // creates a connection to database
        Connection conn = H2Access.createConnection("me", "password");
        // builds a query string to create the table if it doesn't exist
        String query = "CREATE TABLE IF NOT EXISTS accounting_employee("
                    + "ID INT PRIMARY KEY,"
                + ");" ;

        // builds a statement to execute the table creation query
        conn.createStatement().execute(query);

        // opens a buffered reader to ingest the CSV
        try {
            BufferedReader br = new BufferedReader(
                new FileReader("./src/RegExModel/CSVs/accounting_employees.csv")
            );

            // reads the "header" line to skip it
            br.readLine();

            // the line read in from the reader
            String line;

            // keeps going until we run out of lines
            while((line = br.readLine()) != null){
                // split the line
                String[] split = line.split(",");
                // builds a query to insert the data
                query = String.format("INSERT INTO accounting_employee VALUES(%s);", split[0]);
                // creates a statement and executes the query
                conn.createStatement().execute(query);
            }

            // close the reader after we're done with the file
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the package table from the package CSV.
     *
     * @throws SQLException if any number of  things go wrong, from failure to establish a connection to failure to execute a query.
     */
    private static void packageInit() throws SQLException{
        // creates a connection to the db
        Connection conn = H2Access.createConnection("me", "password");

        // builds a query to create the table if it doesn't exist
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
        
        // builds a statement and executes the table creation
        conn.createStatement().execute(query);
        
        // next we have to ingest the data from the CSV
        try {
            // creates a new buffered reader
            BufferedReader br = new BufferedReader(
                new FileReader("./src/RegExModel/CSVs/package.csv")
            );
            
            // skips the "header" line in the CSV
            br.readLine();
            
            // the line being read
            String line;
            // keeps going until we run out of lines
            while((line = br.readLine()) != null){
                // split the line
                String[] split = line.split(",");
                // builds a query to execute
                query = String.format(
                    "INSERT INTO package VALUES(%s,%s,'%s',%s,%s,%s,%s,'%s','%s','%s');",
                    split[0], 
                    split[1], 
                    split[2], 
                    split[3], 
                    split[4], 
                    split[5], 
                    split[6], 
                    split[7], 
                    "null", 
                    "null"
                );
                
                // inserts the row
                conn.createStatement().execute(query);
            }
            
            // after all rows read in, close the reader
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the customer table from the customer CSV.
     * 
     * @throws SQLException if any number of things go wrong, from failure to establish a connection to failure to execute a query.
     */
    private static void customers() throws SQLException {
        // creates a connection to the database
        Connection conn = H2Access.createConnection("me", "password");
        // builds a query to create the table if it doesn't exist
        String query = "CREATE TABLE IF NOT EXISTS customer("
                    + "ACCOUNT_NUMBER INT PRIMARY KEY auto_increment,"
                    + "BILLING_FK INT,"
                    + "NEGOTIATED_RATE_ID_FK INT,"
                    + "MAILING_ADDRESS_ID_FK INT,"
                    + "FIRST_NAME VARCHAR(255),"
                    + "LAST_NAME VARCHAR(255),"
                    + "PHONE_NO VARCHAR(255),"
                + ");" ;

        // creates and executes table creation query
        conn.createStatement().execute(query);

        // goes through the CSV and ingests the data
        try {
            // opens a reader
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/customer.csv"));
            String line;
            String account_num, billing_fk, negotiated_rate_ID_fk, mailing_address_ID_fk;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("account_number")) {
                    account_num = split[0].substring(1, split[0].length()-1);
                    billing_fk = split[1].substring(1, split[1].length()-1);
                    negotiated_rate_ID_fk = split[2].substring(1, split[2].length()-1);
                    mailing_address_ID_fk = split[3].substring(1, split[3].length()-1);
                    query = String.format(
                        "INSERT INTO customer VALUES(%s,%s,%s,%s,'%s','%s','%s');",
                        account_num,
                        billing_fk,
                        negotiated_rate_ID_fk,
                        mailing_address_ID_fk,
                        split[4],
                        split[5],
                        split[6]
                    );
                    conn.createStatement().execute(query);
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sets up the billing table from the billing CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void billing() throws SQLException {
        Connection conn = H2Access.createConnection("me", "password");
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
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/billing.csv"));
            String line;
            while((line = br.readLine()) != null){
                String[] split = line.split(",");
                if(!split[0].equals("ID")) {
                    String payModel = split[2].substring(1, split[2].length()-1);
                    query = String.format("INSERT INTO billing VALUES(%d, %f,'%s',%d,%d);",
                            Integer.parseInt(split[0].replace("\"","")),
                            Double.parseDouble(split[1].replace("\"","")),
                            payModel,
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
     * Sets up the user table from the user CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void users() throws  SQLException{
        Connection conn = H2Access.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS user("
                + "GENERAL_FK INT,"
                + "USERNAME VARCHAR(255) PRIMARY KEY,"
                + "PASSWORD VARCHAR(255),"
                + "TYPE VARCHAR(255)"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/user.csv"));
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
     * Sets up the address table from the address CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void address() throws SQLException{
        Connection conn = H2Access.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS address("
                // Autoincrement for easy of adding future addresses
                + "ID INT PRIMARY KEY auto_increment,"
                + "COMPANY VARCHAR(255),"
                + "ATTN VARCHAR(255),"
                + "STREET_LINE_1 VARCHAR(255),"
                + "STREET_LINE_2 VARCHAR(255),"
                + "ZIP_FK INT,"
                + "ACCOUNT_NUMBER_FK INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/address.csv"));
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
     * Sets up the charges table from the charges CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void charges() throws SQLException{
        Connection conn = H2Access.createConnection("me", "password");
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
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/charges.csv"));
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
     * Sets up the location table from the location CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void location() throws SQLException{
        Connection conn = H2Access.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS location("
                + "ID CHAR(12) PRIMARY KEY,"
                + "ADDRESS_ID INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/location.csv"));
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
     * Sets up the priority table from the priority CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void priority() throws SQLException{
        Connection conn = H2Access.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS priority("
                + "ID INT PRIMARY KEY,"
                + "AIR_GROUND INT,"
                + "RUSH INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/priority.csv"));
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
     * Sets up the rate table from the rates CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void rates() throws SQLException{
        Connection conn = H2Access.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS rate("
                + "NEGOTIATED_RATE_ID INT PRIMARY KEY auto_increment,"
                + "GROUND_RATE DOUBLE,"
                + "AIR_RATE DOUBLE,"
                + "RUSH_RATE DOUBLE,"
                + "DIM_RATING_BREAK INT,"
                + "EMPLOYEE_ID INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/rates.csv"));
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
     * Sets up the service table from the service CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void service() throws SQLException{
        Connection conn = H2Access.createConnection("me", "password");
        String query = "CREATE TABLE IF NOT EXISTS service("
                + "ID INT PRIMARY KEY,"
                + "PRIORITY_FK INT,"
                + "HAZARDOUS INT,"
                + "SIGNATURE_REQ INT,"
                + ");" ;
        Statement stmt = conn.createStatement();
        stmt.execute(query);
        try {
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/service.csv"));
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
     * Sets up the zip_code table from the zipcodes CSV
     * @throws SQLException if any number of  things go wrong, from
     * failure to establish a connection to failure to execute a query.
     */
    private static void zipCodes() throws SQLException{
        Connection conn = H2Access.createConnection("me", "password");
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
            BufferedReader br = new BufferedReader(new FileReader("./src/RegExModel/CSVs/zipcodes.csv"));
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

    public static void permissions() {
        try {
            Connection conn = H2Access.createConnection("me", "password");
            Statement stmt = conn.createStatement();
            // gives customer their correct permission
            String query = "GRANT ALL ON customer, address, zip_code, billing, rate, package, transaction TO PUBLIC";
            stmt.execute(query);
        } catch (SQLException sqle) {
            /* well let's hope this doesn't happen :/ */
        }
    }


    /**
     * Simple method to initialize all of the database tables to their "default" state
     */
    public static void initDatabase(){
        try {
            // runs all of the methods to initialize the tables
            transaction();
            packageInit();
            customers();
            billing();
            accountingEmployees();
            packageEmployees();
            address();
            charges();
            location();
            priority();
            rates();
            service();
            zipCodes();
            users();
            permissions();
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
