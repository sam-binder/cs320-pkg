package RegExModel;
import com.sun.org.apache.xpath.internal.SourceTree;
import sun.font.TrueTypeFont;

import java.io.IOException;
import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.Time;
import java.sql.SQLException;
import java.util.Scanner;

/**
 * Created by Walter Schaertl on 3/23/2019.
 */

/**
 * Class that contains ease of use methods for the employees that use the system.
 * Functions here are called by the server.
 */
public class EmployeeAccess implements AutoCloseable{
    private String username, type;
    private Connection connection;
    private H2Access h2;

    /**
     * How an employee logs in
     * @param username the employee's username
     * @param password the employee's password
     * @param type the type of employee (accounting or package)
     * @throws SQLException Indicates a failure to log in, either wrong username or password
     */
    public EmployeeAccess(String username, String password, String type) throws SQLException{
        this.username = username;
        this.type = type;
        this.h2 = new H2Access();
        this.connection = this.h2.createConnection(username, password);
    }

    /**
     * An easy way to get the ID of a logged in employee
     * @return int: The employee's ID. returns -1 on failure.
     */
    public int getId() {
        String query = "SELECT general_fk FROM user where username='" + username + "'";
        ResultSet r = h2.createAndExecuteQuery(connection, query);
        try {
            if (r.next())
                return r.getInt(1);
        }catch (SQLException e) {}
        return -1;
    }

    /**
     * An easy way to get the ID of a user
     * @return int: The employee's ID. returns -1 on failure.
     */
    public int getId(String username) {
        String query = "SELECT general_fk FROM user where username='" + username + "'";
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try {
            if (r.next())
                return r.getInt(1);
        }catch (SQLException e) {}
        return -1;
    }

    /**
     * Function required to be implemented by AutoCloseable.
     * Lets CustomerAccess be used in a 'try with resources' block
     */
    @Override
    public void close(){
        h2.closeConnection(this.connection);
    }

    /**
     * Ease of use function that lets logged in package employees update a packages location.
     * Makes an entry in the transaction table.
     *
     * @param locationId The location the package employee is at
     * @param acctNumber The account number assocaited with the package
     * @param pkgSerial The package serial
     */
    public void updatePackageLocation(String locationId, int acctNumber, String pkgSerial){
        Date date = new Date(System.currentTimeMillis());
        Time time = new Time(System.currentTimeMillis());
        int employeeId = getId();
        String query = String.format("INSERT INTO transaction(DATE, TIME, EMPLOYEE_ID_FK, LOCATION_ID_FK, " +
                        "ACCOUNT_NUMBER_FK, PACKAGE_SERIAL_FK) VALUES (\'%s\',\'%s\',\'%s\',\'%s\',\'%s\',\'%s\');",
                        date, time, employeeId, locationId, acctNumber, pkgSerial);
        h2.createAndExecute(connection, query);
    }

    /**
     * An easy way for accounting employees to see all the customers in the system to
     * better aid them with billing or questions.
     * @return a ResultSet of customers, to be processed by the client
     */
    public ResultSet getCustomers(){
        String query = "SELECT * from customer";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * An easy way for accounting employees to see all the customers in the system,
     * and allows for selection and trimming down of results.
     * @return a ResultSet of customers, to be processed by the client
     */
    public ResultSet getCustomersWhere(String conditional){
        String query = "SELECT * from customer WHERE " +  conditional + ";";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * The method to view the customer info by customer account, first name and last name.
     * @param customerID the account for the customer
     * @param lastName last name
     * @param firstName first name
     * @return the result set of the specific customer
     */
    public ResultSet viewSpecificCustomer(String customerID, String lastName, String firstName) {
        int accountNum = Integer.parseInt(customerID);
        String query = "SELECT * FROM customer WHERE account_number = " + customerID + " AND" +
                " last_name = " + lastName + " AND first_name = " + firstName + ";";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * An easy way for accounting employees to see customer billing information
     * @param acctNumber int: The id of the account to view the billing information
     * @return a ResultSet of customer billing information, to be processed by the client
     */
    public ResultSet viewCustomerBilling(int acctNumber){
        String query = "SELECT * from billing WHERE account_number_fk=" + acctNumber + ";";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * The way to modify the billing info for the customers
     * @param ID the id for billing table
     * @param balance the new balance for specific customers
     * @param payModel the new pay model for specific coustomers
     * @param acctNum the account number for the customer
     */
    public void modifyCustomerBilling(int ID, double balance, String payModel, int acctNum) {
        int employeeID = this.getId();
        String query = "UPDATE billing SET balance_to_date=" + balance + " , pay_model=" + payModel +
                ", employeeID=" + employeeID + "WHERE ID=" + ID + " AND account_number_fk=" + acctNum + ";";
        h2.createAndExecute(connection, query);
    }

    /**
     * An easy way for accounting employee's to track a package
     * @param accntNum: The account id associate with the package
     * @param serial: The package's serial
     * @return A ResultSet of transactions showing the locations a package has been
     */
    public ResultSet viewPackageHistory(int accntNum, String serial){
        String query = "SELECT * from transaction WHERE account_number_fk=" + accntNum +
                " AND package_serial_fk='" + serial +"';";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * An easy way to view detailed information about a package.
     * @param accntNum: The account id associate with the package
     * @param serial: The package's serial
     * @return A ResultSet of the package.
     */
    public ResultSet viewPackageData(int accntNum, String serial){
        String query = "SELECT * from package WHERE account_number_fk=" + accntNum +
                " AND serial='" + serial +"';";
        return h2.createAndExecuteQuery(connection, query);
    }


    /**
     * The way to create an entry to rates table.
     * @param negotiatedID the negotiated id
     * @param groundRate the ground rate of the package
     * @param airRate the air rate of the package
     * @param rushRate the rush rate of the package
     * @param DRB dim_rating_break
     */
    public void CreateRate(String negotiatedID, String groundRate, String airRate, String rushRate, String DRB) {
        int employeeID = this.getId();
        int negotiated = Integer.parseInt(negotiatedID);
        double ground = Double.parseDouble(groundRate);
        double air = Double.parseDouble(airRate);
        double rush = Double.parseDouble(rushRate);
        int drb = Integer.parseInt(DRB);
        String query = String.format("INSERT INTO rates(NEGOTIATED_RATE_ID, GROUND_RATE, AIR_RATE, RUSH_RATE, " +
                "DIM_RATING_BREAK, EMPLOYEEID) VALUES (\"%d\",\"%f\",\"%f\",\"%f\",\"%d\",\"%d\");",
                negotiated, ground, air, rush, drb, employeeID);
        h2.createAndExecute(connection, query);
    }

    /**
     * The way to view the rates of the package by negotiated id
     * @param negotiatedID the negotiated id for the package
     * @return A ResultSet of the rates.
     */
    public ResultSet viewRates(String negotiatedID) {
        int negotiated = Integer.parseInt(negotiatedID);
        String query = "SELECT * FROM rate WHERE negotiated_rate_id=" + negotiated + ";";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * The way to modify the info of the entry from rates table
     * @param negotiatedID the negotiated id
     * @param groundRate the new ground rate to be set up
     * @param airRate the new air rate to be set up
     * @param rushRate the new rush rate to be set up
     * @param DRB the new dim_rating_break to be set up
     */
    public void modifyRates(String negotiatedID, String groundRate, String airRate, String rushRate, String DRB) {
        int employeeID = this.getId();
        int negotiated = Integer.parseInt(negotiatedID);
        double ground = Double.parseDouble(groundRate);
        double air = Double.parseDouble(airRate);
        double rush = Double.parseDouble(rushRate);
        int drb = Integer.parseInt(DRB);
        String query = String.format("UPDATE rates SET GROUND_RATE = \"" + ground + "\", AIR_RATE = \"" +
                air + "\", RUSH_rATE = \"" + rush + "\", DIM_RATING_BREAK = \"" + drb + "\", EMPLOYEEID =\""
                + employeeID + "WHERE NEGOTIATED_RATE_ID = \"" + negotiated + "\";");

        h2.createAndExecute(connection, query);
    }

    /**
     * The method to allow the employee to view the charging info with specific account number, id and serial
     * number of the package
     * @param ID the id of the charge table
     * @param account_num the account number for specific customers
     * @param package_serial the serial of the package
     * @return the ResultSet of the charge table
     */
    public ResultSet viewCharge(int ID, int account_num, int package_serial) {
        String query = "SELECT price FROM charges WHERE ID = " + ID + " AND account_number_fk = " +
                account_num + " AND package_serial_fk = " + package_serial + ";";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * The method for employees to update the charge for the package.
     * @param ID the ID of the charges table
     * @param account_num the account number of customers
     * @param package_serial the package serial
     * @param new_charges the new charge for the package
     */
    public void modifyCharge(int ID, int account_num, int package_serial, int new_charges, int service_id) {
        String query = "UPDATE charges SET price = " + new_charges + ", service_id = " + service_id +
                " WHERE ID = " + ID + " AND account_number_fk = " + account_num +
                " AND package_serial_fk = " + package_serial + ";";
        h2.createAndExecute(connection, query);
    }

    /**
     * The SQL for viewing the current location of the package is at
     * @param account_num the account number of the customer
     * @param serial the serial of the package
     * @return the ResultSet of the package current location
     */
    public ResultSet viewPackageCurrentLocation(int account_num, String serial) {
        String query = "SELECT location_ID_fk FROM transaction WHERE account_number_fk = " + account_num +
                " AND package_serial_fk = " + serial + " AND ID = SELECT MAX(T.ID) FROM transaction AS T WHERE " +
                "T.account_number_fk = " + account_num + " AND package_serial_fk = '" + serial + "';";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * The method to insert to signature of receiver
     * @param receiver name of receiver
     * @param account_num account number of the customer
     * @param serial serial of the package
     */
    public void putSignature(String receiver, String account_num, String serial) {
        String query = "UPDATE package SET signed_for_by = '" + receiver + "' WHERE account_number_fk = " +
                account_num + " AND serial = '" + serial + "';";
        h2.createAndExecute(connection, query);
    }

    /**
     * Check the receiver's name if the package is signed
     * @param account_num the account number of the customer
     * @param serial the serial of the package
     * @return the resultSet of the signature of the package
     */
    public ResultSet checkSignature(String account_num, String serial) {
        String query = "SELECT signed_for_by FROM  package WHERE account_number_fk = " + account_num +
                " AND serial = " + serial + ";";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * View the service of the package the customer pick
     * @param account_num the account number fo the customer
     * @param serial the serial of the package
     * @return the resultSet of the service of the package
     */
    public ResultSet viewService(int account_num, String serial) {
        String query = "SELECT * FROM service WHERE ID = (SELECT service_id_fk FROM package WHERE" +
                " account_number_fk = " + account_num + " AND serial = '" + serial + "');";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * Method to view the priority of the package
     * @param service_id the service id
     * @return the resultSet of priority
     */
    public ResultSet viewPriority(int service_id) {
        String query = "SELECT * FROM priority WHERE ID = (SELECT priority_fk FROM service WHERE " +
                "ID = " + service_id + ";";
        return h2.createAndExecuteQuery(connection, query);
    }

    public ResultSet viewAddress(int maillingId){
        String query = "SELECT * FROM address WHERE id=" + maillingId;
        return H2Access.createAndExecuteQuery(connection, query);
    }

    public ResultSet viewZip(int zipId){
        String query = "SELECT * FROM zip_code WHERE id=" + zipId;
        return H2Access.createAndExecuteQuery(connection, query);
    }

    public String getPackageDestination(int acctNumber, String pkgSerial){
        // If it doesn't exist, return None
        if(!testpackageId(acctNumber, pkgSerial))
            return null;
        String query = "SELECT destination_fk FROM package WHERE account_number_fk=%d and serial='%s'";
        query = String.format(query, acctNumber, pkgSerial);
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try{
            if(r.next())
                return r.getString(1);
        } catch (SQLException e){
            return null;
        }
        return null;
    }

    /**
     * Tests if this location ID is valid
     * @param locationID
     * @return
     */
    private boolean testLocationId(String locationID){
        String query = "SELECT * FROM location WHERE id='"+ locationID +"';";
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try {
            return r.next();
        } catch (SQLException e){
            return  false;
        }
    }

    private boolean testpackageId(int acctNumber, String pkgSerial){
        String query = "SELECT * FROM package WHERE account_number_fk=%d and serial='%s'";
        query = String.format(query, acctNumber, pkgSerial);
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try{
            return r.next();
        } catch (SQLException e){
            return false;
        }
    }

    private void clearScreen(){
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e){}

        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     *
     * @param in: A scanner to get user input
     * @param locationID: The location Id of the employee (must be either a hub or vehicle). Ignored
     *                  if dropOff (as in to it's final destination) is true
     * @param dropOff: If this package is being dropped off
     */
    private void scanPackage(Scanner in, String locationID, boolean dropOff){
        String pkg = "";
        // While drivers don't switch from scanning in to going out on delivery,
        // and hub employee's haven't quit.
        while(true){
            // Scan the package, Sample Package: 00013114B9IWEA9
            System.out.print("Please scan a package: ");
            pkg = in.nextLine();
            if(pkg.equalsIgnoreCase("T") || pkg.equalsIgnoreCase("Q"))
                break;

            // Make sure the ID is correctly formatted
            if(pkg.length() < 15 && !(pkg.equalsIgnoreCase("T") || pkg.equalsIgnoreCase("Q"))){
                System.out.println("Not enough digits entered.");
            } else if(pkg.length() > 15){
                System.out.println("Too many digits entered");
            } else {
                // Get package information
                int acctNum = Integer.parseInt(pkg.substring(0, 6));
                String serial = pkg.substring(8, 14);
                if ((!testpackageId(acctNum, serial))||(Util.validateCheckDigit(pkg))){
                    System.out.println("That package does not exists." +
                            " Please verify the information was entered correctly.");
                    return;
                }
                if(!dropOff) {
                    // Get the account number and serial from the package
                    updatePackageLocation(locationID, acctNum, serial);
                    System.out.println("Package location updated!");
                } else {
                    // Add a transaction that this package has reached it's destination
                    ResultSet r = viewService(acctNum, serial);
                    try{
                        if(r.next() && r.getInt("Signature_req") == 1){
                            System.out.print("A signature is required, " +
                                    "please have the receiver sign their name: ");
                            String signature = in.nextLine();
                            // Update the package with a signature
                            putSignature(signature, acctNum+"", serial);
                        }

                        // Update the package with its destination
                        updatePackageLocation(getPackageDestination(acctNum, serial), acctNum, serial);
                        System.out.println("Package successfully delivered!");
                    } catch (SQLException e){
                        e.printStackTrace();
                        System.out.println("A fault has been detected with the database. Please" +
                                " contact a system administrator.");
                    }
                }
            }
        }
    }

    private void printCustomer(ResultSet customers){
        try {
            int zipId = 0;
            int acctNum = customers.getInt("account_number");
            int addressID = customers.getInt("mailing_address_id_fk");
            String rateId = customers.getInt("negotiated_rate_id_fk") + "";

            do {
                System.out.println("Personal Information");
                System.out.println("\tFirst Name: " + customers.getString("first_name"));
                System.out.println("\tLast Name: " + customers.getString("last_name"));
                System.out.println("\tPhone Number: " + customers.getString("phone_no"));
                System.out.println("\tAccount Number: " + acctNum);
                System.out.println("Billing Information: ");
                ResultSet billing = viewCustomerBilling(acctNum);
                if(billing.next()){
                    System.out.println("\tCurrent Balance: " + billing.getDouble("balance_to_date"));
                    System.out.println("\tPayment Model: " + billing.getString("pay_model"));
                } else
                    System.out.println("No billing information for this customer.");
                System.out.println("Negotiated Rates:");
                ResultSet rates = viewRates(rateId);
                if(rates.next()){
                    System.out.println("\tGround rate: " + rates.getDouble("ground_rate"));
                    System.out.println("\tAir rate: " + rates.getString("air_rate"));
                    System.out.println("\tRush rate: " + rates.getString("rush_rate"));
                    System.out.println("\tDimension Rating Break: " + rates.getInt("dim_rating_break"));
                } else
                    System.out.println("No negotiated rates for this customer.");
                System.out.println("Home address:");
                ResultSet address = viewAddress(addressID);
                if(address.next()) {
                    System.out.println("\tCompany: " + address.getString("company"));
                    System.out.println("\tAttention of: " + address.getString("attn"));
                    System.out.println("\tStreet Line One: " + address.getString("street_line_1"));
                    System.out.println("\tStreet Line Two: " + address.getString("street_line_2"));
                    zipId = address.getInt("zip_fk");
                    if (zipId > 0) {
                        ResultSet zip = viewZip(zipId);
                        if (zip.next()) {
                            System.out.println("\tZip Code: " + zip.getInt("zip_code"));
                            System.out.println("\tCity: " + zip.getString("city"));
                            System.out.println("\tState: " + zip.getString("state"));
                        }
                    }
                } else
                    System.out.println("No home address information for this customer.");
            } while (customers.next());
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    private String getUserInput(String[] choices){
        while(true) {
            Scanner in = new Scanner(System.in);
            String userString = in.nextLine();
            for (String choice : choices)
                if (userString.equalsIgnoreCase(choice))
                    return choice;
            System.out.print("Error: that input was not recognized. Please try again:");
        }
    }

    private void basePage(){
        System.out.println("[H] Home    [B] Back    [Q]    quit");
    }
    private void acctHomePage(){
        // Generate Invoices
        // Make a new rate
        // Edit billing
        basePage();
        System.out.println("[1] Generate customer invoices.");
        System.out.println("[2] Edit a customers billing.");
        System.out.println("[3] Track a package.");
        System.out.print("Please enter a choice: ");
        String userStr = getUserInput(new String[]{"1", "2", "3", "H", "B", "Q"});
        switch (userStr){
            case "H":
                acctHomePage();
                break;
            case "B":
                break;
            case "Q":
                System.exit(0);
                break;
            case "1":
                generateCustomerInvoices();
                acctHomePage();
                break;
            case "2":
                //editCustomerBilling();
                break;
            case "3":
                //trackAPackage();
                break;
        }

    }

    private void generateCustomerInvoices(){
        Scanner in = new Scanner(System.in);
        String desired = "";
        int customerId = 0;
        while(!desired.equals("Y")) {
            basePage();
            System.out.println("Which customer would you like to generate an invoice for?");
            System.out.println("[1] First and Last name");
            System.out.println("[2] Account number");
            System.out.println("[3] Customer username");
            //System.out.println("[4] All based on pay period");
            System.out.print("Please enter the number of how you'd like to select a customer: ");
            String method = getUserInput(new String[]{"1", "2", "3", "4"});
            ResultSet r;
            String query = "";
            switch (method) {
                case "1":
                    String[] name;
                    do {
                        System.out.print("Please enter the First and Last name: ");
                        name = in.nextLine().split(" ");
                        if (name.length != 2)
                            System.out.println("Please enter exactly two names.");
                    } while(name.length != 2);
                    query = String.format("first_name='%s' AND last_name='%s'", name[0], name[1]);
                    break;
                case "2":
                    System.out.print("Please enter the account number: ");
                    int acctNum = in.nextInt();
                    query = "account_number=" + acctNum;
                    break;
                case "3":
                    System.out.print("Please enter the user name: ");
                    String username = in.nextLine();
                    query = "account_number=" + getId(username);
                    break;
//                case "4":
//                    System.out.println("Please enter the pay period you'd like to generate" +
//                            "charges for.");
//                    String[] choices = {"annually", "bi-annually", "quarterly", "monthly"};
//                    String payPeriod = getUserInput(choices);
//                    query = "";
//                    // TODO this
            }
            r = getCustomersWhere(query);
            try {
                if (!r.next())
                    System.out.println("No customer found matching that information. Please try again.");
                else {
                    printCustomer(r);
                    System.out.print("Is this the desired user? [Y/N]");
                    desired = getUserInput(new String[]{"Y", "N"});
                    if(desired.equalsIgnoreCase("Y")) {
                        r.beforeFirst();
                        if(r.next())
                            customerId = r.getInt("account_number");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Generating Invoice...");

        int numCharges = 0;
        double amountDue = 0;
        double accountBalance = getBalance(customerId);
        double data[] = viewCharges(customerId);
        if(data != null) {
            numCharges = (int) data[0];
            amountDue = data[1];
        }

        System.out.printf("This user has %d unpaid charge(s) totalling $%.2f.\n", numCharges, amountDue);
        if(numCharges > 0) {
            System.out.print("Continue to pay these charges? [Y/N]: ");
            String cont = getUserInput(new String[]{"Y", "N"});
            if (cont.equalsIgnoreCase("Y")) {
                double newBalance = payCharges(customerId, amountDue);
                if (Double.isNaN(newBalance)) {
                    System.out.println("An error has occurred while trying to create this invoice.\n" +
                            "Please contact a system administrator.");
                } else {
                    System.out.printf("The customer now has a balance of %.2f\n", newBalance);
                    if (newBalance < 0) {
                        System.out.println("Note: This customer lacked the funds in their account, " +
                                "and requires a bill to be mailed to their main address.\n" +
                                "This customer will no longer be able to send packages until their" +
                                " balance is positive again.");
                    }
                }
            } else {
                System.out.println("Charges not paid.");
            }
        } else {
            System.out.println("No action available.");
        }

    }

    private double getBalance(int acct){
        double newBalance = Double.NaN;
        String getBalance = "SELECT balance_to_date FROM billing WHERE account_number_fk = " + acct;
        ResultSet currentBalance = H2Access.createAndExecuteQuery(connection, getBalance);
        try {
            if(currentBalance.next())
                newBalance = currentBalance.getDouble(1);
        } catch (SQLException e){
            e.printStackTrace();
        }
        return  newBalance;
    }

    private double payCharges(int acct, double amountDue){
        double newBalance = getBalance(acct);
        if(!Double.isNaN(newBalance)){
            newBalance -= amountDue;
            String payChargeQuery = "UPDATE charge SET paid = 1 where account_number_fk = " + acct;
            String editBalanceQuery = "UPDATE billing SET balance_to_date = %f WHERE account_number_fk = %d";
            H2Access.createAndExecute(connection, payChargeQuery);
            H2Access.createAndExecute(connection, String.format(editBalanceQuery, newBalance, acct));
        }
        return newBalance;
    }

    private double[] viewCharges(int accountID){
        String detailQuery = "select * from charge where account_number_fk = " + accountID;
        String metaQuery = "SELECT COUNT(id), SUM(price) FROM CHARGE WHERE account_number_fk = "
                + accountID + " AND paid = 0;";
        ResultSet r = H2Access.createAndExecuteQuery(connection, detailQuery);
        ResultSet r2 = H2Access.createAndExecuteQuery(connection, metaQuery);

        try{
            if(r != null && r.next()){
                System.out.println("Charges for this Customer:");
                System.out.println("Amount\t\tPackage Serial\t\tPaid");
                do {
                    System.out.print(r.getDouble("price") + "\t\t\t");
                    System.out.print(r.getString("package_serial_fk") + "\t\t\t");
                    System.out.println(r.getInt("paid") == 1 ? " Yes" : " No");
                } while (r.next());
            }
            if(r2 != null && r2.next()) {
                return new double[]{r2.getInt(1), r2.getDouble(2)};
            }
        } catch (SQLException e){
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args){
        Scanner in = new Scanner(System.in);
        boolean success = false;
        do {
            // Get the username and password
            System.out.print("Username: ");
            String username = in.nextLine();
            System.out.print("Password: ");
            String password = in.nextLine();
            try {
                // Get the employee's type of user
                String employeeType =  H2Access.getUserType(username);
                // If the username doesn't exists in the system, reenter credentials
                if(employeeType == null)
                    System.out.println("This username doesn't exists, please try again.");
                // Customers are not allowed to enter this view
                else if(employeeType.equalsIgnoreCase("customer"))
                    System.out.println("Customers are not allowed to access this portal.");
                else {
                    // Log the employee in
                    EmployeeAccess employee = new EmployeeAccess(username, password, employeeType);
                    success = true;
                    System.out.println("Congratulations! You've been logged in as a(n) " +
                            employeeType.replace("_", " ") + ".");
                    // Package employee view
                    if(employeeType.equalsIgnoreCase("package_employee")){
                        boolean idSuccess;
                        do {
                            // Sample number: VGLJPA1YKP5G
                            System.out.print("Please enter your location ID: ");
                            String locationID = in.nextLine();

                            // Test if this is a valid location
                            idSuccess = employee.testLocationId(locationID);
                            if (!employee.testLocationId(locationID)) {
                                System.out.println("That location does not exists, please try again.");
                            }
                            // Employee's can only work at hubs or in vehicles
                            else if (locationID.startsWith("TD") || locationID.startsWith("TO")){
                                System.out.println("Employees cannot sign into origins or destinations.");
                            }
                            // Employees working at hubs just scan packages, employees who work on
                            // vehicles first scan packages onto the truck, then off for delivery.
                            else {
                                if(locationID.startsWith("H")) {
                                    // workers at hubs just scan
                                    System.out.println("Tracking number received, " +
                                            "you may now start scanning packages (or Q to quit).");
                                    employee.scanPackage(in, locationID, false);
                                }else {
                                    System.out.println("You are currently scanning packages to load" +
                                            " on to your truck. Press T to go out on delivery.");
                                    employee.scanPackage(in, locationID, false);
                                    System.out.println("You are currently out on delivery. " +
                                            "Please scan packages as they're dropped off (or Q to quit).");
                                    employee.scanPackage(in, locationID, true);
                                }
                            }
                        }
                        while (!idSuccess);
                    } else if(employeeType.equalsIgnoreCase("accounting_employee")){
                        employee.acctHomePage();
                    }
                }
            } catch (SQLException e){
                System.out.println("Could not log in with those credentials. " +
                        "Check your username and password. " +
                        "If the issue persists, contact an administrator.");
            }
        } while(!success);
    }
}
