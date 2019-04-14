package RegExModel;

import java.io.IOException;
import java.sql.*;
import java.util.Scanner;


/**
 * Class that contains ease of use methods for the employees that use the system.
 * Functions here are called by the server.
 */
public class EmployeeAccess implements AutoCloseable{
    private String username, type;
    private Connection connection;

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
        this.connection = H2Access.createConnection(username, password);
    }

    /**
     * An easy way to get the ID of a logged in employee
     * @return int: The employee's ID. returns -1 on failure.
     */
    public int getId() {
        String query = "SELECT general_fk FROM user where username='" + username + "'";
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try {
            if(r != null && r.next())
                return r.getInt(1);
        } catch (SQLException e) {}
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
            if (r != null && r.next())
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
        H2Access.closeConnection(this.connection);
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
        H2Access.createAndExecute(connection, query);
    }

    /**
     * An easy way for accounting employees to see all the customers in the system to
     * better aid them with billing or questions.
     * @return a ResultSet of customers, to be processed by the client
     */
    public ResultSet getCustomers(){
        String query = "SELECT * from customer";
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * An easy way for accounting employees to see all the customers in the system,
     * and allows for selection and trimming down of results.
     * @return a ResultSet of customers, to be processed by the client
     */
    public ResultSet getCustomersWhere(String conditional){
        String query = "SELECT * from customer WHERE " +  conditional + ";";
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * The method to view the customer info by customer account, first name and last name.
     * @param customerID the account for the customer
     * @param lastName last name
     * @param firstName first name
     * @return the result set of the specific customer
     */
    public ResultSet viewSpecificCustomer(String customerID, String lastName, String firstName) {
        String query = "SELECT * FROM customer WHERE account_number = " + customerID + " AND" +
                " last_name = " + lastName + " AND first_name = " + firstName + ";";
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * An easy way for accounting employees to see customer billing information
     * @param acctNumber int: The id of the account to view the billing information
     * @return a ResultSet of customer billing information, to be processed by the client
     */
    public ResultSet viewCustomerBilling(int acctNumber){
        String query = "SELECT * from billing WHERE account_number_fk=" + acctNumber + ";";
        return H2Access.createAndExecuteQuery(connection, query);
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
        H2Access.createAndExecute(connection, query);
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
        return H2Access.createAndExecuteQuery(connection, query);
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
        return H2Access.createAndExecuteQuery(connection, query);
    }


    /**
     * The way to create an entry to rates table.
     * @param groundRate the ground rate of the package
     * @param airRate the air rate of the package
     * @param rushRate the rush rate of the package
     * @param DRB dim_rating_break
     * @return the PK of the new rate
     */
    public int CreateRate(double groundRate, double airRate, double rushRate, int DRB) {
        int employeeID = this.getId();
        String query = String.format("INSERT INTO rate(GROUND_RATE, AIR_RATE, RUSH_RATE, " +
                        "DIM_RATING_BREAK, EMPLOYEE_ID) VALUES (%f, %f, %f, %d, %d);",
                groundRate, airRate, rushRate, DRB, employeeID);
        try {
            PreparedStatement prep = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            prep.executeUpdate();
            ResultSet keys = prep.getGeneratedKeys();
            if (keys.next())
                return keys.getInt("negotiated_rate_id");
            else
                return -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    /**
     * The way to view the rates of the package by negotiated id
     * @param negotiatedID the negotiated id for the package
     * @return A ResultSet of the rates.
     */
    public ResultSet viewRates(String negotiatedID) {
        int negotiated = Integer.parseInt(negotiatedID);
        String query = "SELECT * FROM rate WHERE negotiated_rate_id=" + negotiated + ";";
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * The way to modify the info of the entry from rates table
     * @param negotiatedID the negotiated id
     * @param groundRate the new ground rate to be set up
     * @param airRate the new air rate to be set up
     * @param rushRate the new rush rate to be set up
     * @param DRB the new dim_rating_break to be set up
     */
    public void modifyRates(int negotiatedID, double groundRate, double airRate, double rushRate, int DRB) {
        int employeeID = this.getId();
        String query = String.format("UPDATE rate SET GROUND_RATE=%f, AIR_RATE=%f, RUSH_RATE=%f, " +
                "DIM_RATING_BREAK=%d, EMPLOYEE_ID=%d WHERE negotiated_rate_id=%d;", groundRate,
                airRate, rushRate, DRB, employeeID, negotiatedID);
        H2Access.createAndExecute(connection, query);
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
        return H2Access.createAndExecuteQuery(connection, query);
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
        H2Access.createAndExecute(connection, query);
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
        return H2Access.createAndExecuteQuery(connection, query);
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
        H2Access.createAndExecute(connection, query);
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
        return H2Access.createAndExecuteQuery(connection, query);
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
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * Method to view the priority of the package
     * @param service_id the service id
     * @return the resultSet of priority
     */
    public ResultSet viewPriority(int service_id) {
        String query = "SELECT * FROM priority WHERE ID = (SELECT priority_fk FROM service WHERE " +
                "ID = " + service_id + ";";
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * Method to view the mailing address information when given the ID
     * @param maillingId the primary key
     * @return a result set of data associated with that key
     */
    public ResultSet viewAddress(int maillingId){
        String query = "SELECT * FROM address WHERE id=" + maillingId;
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * Method to view the zip code information when given the ID
     * @param zipId the primary key
     * @return a result set of data associated with that key
     */
    public ResultSet viewZip(int zipId){
        String query = "SELECT * FROM zip_code WHERE id=" + zipId;
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * When given a packages account number and serial, return the
     * destination of the package.
     * @param acctNumber The account number of the package
     * @param pkgSerial The package serial
     * @return A string of the location Id for the packages destination
     */
    public String getPackageDestination(int acctNumber, String pkgSerial){
        // If it doesn't exist, return None
        if(!testpackageId(acctNumber, pkgSerial))
            return null;
        String query = "SELECT destination_fk FROM package WHERE account_number_fk=%d and serial='%s'";
        query = String.format(query, acctNumber, pkgSerial);
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try{
            if(r != null && r.next())
                return r.getString(1);
        } catch (SQLException e){
            return null;
        }
        return null;
    }


    /**
     * Tests if this location ID is valid
     * @param locationID string location
     * @return boolean if locationID is valid
     */
    private boolean testLocationId(String locationID){
        String query = "SELECT * FROM location WHERE id='"+ locationID +"';";
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try {
            return r != null && r.next();
        } catch (SQLException e){
            return  false;
        }
    }

    /**
     * Tests to see if a package matching this account number and serial exists
     * @param acctNumber The account number
     * @param pkgSerial The pacakage serial
     * @return boolean, if the package exists
     */
    private boolean testpackageId(int acctNumber, String pkgSerial){
        String query = "SELECT * FROM package WHERE account_number_fk=%d and serial='%s'";
        query = String.format(query, acctNumber, pkgSerial);
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try{
            return  r != null && r.next();
        } catch (SQLException e){
            return false;
        }
    }

    /**
     * Clears the screen on the CLI for neat formatting
     */
    private void clearScreen(){
        try {
            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
        } catch (IOException | InterruptedException e){
            e.printStackTrace();
        }

        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Scans a package, making a transaction in the table.
     * @param in: A scanner to get user input
     * @param locationID: The location Id of the employee (must be either a hub or vehicle). Ignored
     *                  if dropOff (as in to it's final destination) is true
     * @param dropOff: If this package is being dropped off
     */
    private void scanPackage(Scanner in, String locationID, boolean dropOff){
        String pkg;
        // While drivers don't switch from scanning in to going out on delivery,
        // and hub employee's haven't quit.
        while(true){
            // Scan the package, Sample Package: 00013114B9IWEAV
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
                if (!testpackageId(acctNum, serial) || !Util.validateCheckDigit(pkg)){
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

    /**
     * Prints the necessary information about a customer. Is used to
     * show the account employees a confirmation this is who they want to do things with.
     * @param customers A Result set of all the information related to a customer. Should contain:
     *                  account_number, mailing_address_id_fk, negotiated_rate_id_fkm, first_name,
     *                  last_name, and phone_no.
     */
    private void printCustomer(ResultSet customers){
        try {
            int zipId;
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

    /**
     * An easy way to get user input that's constrained to a set selection of choices
     * @param choices A String array of options to pick
     * @return The option the user selected.
     */
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

    /**
     * The header at the top of each 'page' on the CLI
     */
    private void basePage(){
        System.out.println("[H] Home    [B] Back    [Q]    Quit");
    }

    /**
     * The options available from the home page of an accountant.
     * Back returns back one page, quit returns to log in, home to home page
     */
    private void acctHomePage(){
        System.out.println();
        basePage();
        System.out.println("[1] Generate customer invoices.");
        System.out.println("[2] Edit a customers billing.");
        System.out.println("[3] Negotiate custom rates.");
        System.out.println("[4] Track a package.");
        System.out.print("Please enter a choice: ");
        String userStr = getUserInput(new String[]{"1", "2", "3", "4", "H", "B", "Q"});
        switch (userStr){
            case "H":
                acctHomePage();
                break;
            case "B":
                acctHomePage();
                break;
            case "Q":
                break;
            case "1":
                generateCustomerInvoices();
                acctHomePage();
                break;
            case "2":
                editCustomerBilling();
                acctHomePage();
                break;
            case "3":
                setUpCustomRates();
                acctHomePage();
                break;
            case "4":
                acctTrackPackage();
                acctHomePage();
                break;
        }

    }

    /**
     * A method that lets accountant negotiate new rates for a customer.
     */
    private void setUpCustomRates(){
        Scanner in = new Scanner(System.in);
        String rates[];
        boolean success = false;
        int customerId = selectCustomer("Which customer would you like to edit the rates of?");
        if(customerId == -2 || customerId == -4)
            return;
        else if(customerId == -3){
            userLogin();
        } else {
            basePage();
            do {
                // If the customer has a rate id of 1, it's the default and to edit it a new one
                // must be create. Otherwise, the object can just be modified.
                System.out.println("After negotiating, please enter the new Ground Rate, Air Rate, " +
                        "Rush Rate, and Dimension Rating Break, in that order separated by spaces.");
                System.out.print("New Rates: ");
                rates = in.nextLine().split(" ");
                if (rates.length != 4)
                    System.out.printf("Exactly four numbers are expected, %d were received.\n", rates.length);
                else {
                    try {
                        double groundRate = Double.parseDouble(rates[0]);
                        double airRate = Double.parseDouble(rates[1]);
                        double rushRate = Double.parseDouble(rates[2]);
                        int DRB = Integer.parseInt(rates[3]);
                        int rateId = getRateId(customerId);
                        if (rateId == 1 || rateId == -1) {
                            int newID = CreateRate(groundRate, airRate, rushRate, DRB);
                            String query = "UPDATE customer SET negotiated_rate_id_fk = %d WHERE account_number = %d";
                            H2Access.createAndExecute(connection, String.format(query, newID, customerId));

                        } else {
                            modifyRates(rateId, groundRate, airRate, rushRate, DRB);
                        }
                        success = true;
                    } catch (NumberFormatException e) {
                        System.out.println("The numbers were malformed, please try again.");
                    }
                }
            } while (!success);
        }
    }

    /**
     * Returns the rate primary key associated with a user's account.
     * @param acctNum The account number
     * @return The rate fk number
     */
    private int getRateId(int acctNum){
        String query = "SELECT negotiated_rate_id_fk FROM customer WHERE account_number=" + acctNum;
        ResultSet r = H2Access.createAndExecuteQuery(connection, query);
        try {
            if(r != null && r.next())
                return r.getInt("negotiated_rate_id_fk");
        } catch (SQLException e){
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * A method that lets accounting employees track either a specific package or
     * all the packages a specific customer sent.
     */
    private void acctTrackPackage() {
        Scanner in = new Scanner(System.in);
        System.out.println();
        System.out.println("[1] Track package based on Tracking number.");
        System.out.println("[2] Track package based on Customer.");
        basePage();
        System.out.print("How would you like to track a package: ");
        String choice = getUserInput(new String[]{"1", "2", "H", "B", "Q"});
        boolean success = false;
        boolean back;
        do {
            back = false;
            switch (choice) {
                case "1":
                    do {
                        System.out.print("Enter package tracking ID: ");
                        String pkg = in.nextLine();
                        if (pkg.equalsIgnoreCase("B")) {
                            back = true;
                            break;
                        } else if (pkg.equalsIgnoreCase("H")){
                            acctHomePage();
                            return;
                        } else if(pkg.equalsIgnoreCase("Q")){
                            userLogin();
                            return;
                        }
                        if (pkg.length() != 15)
                            System.out.println("Package tracking IDs must be 15 digits long");
                        else {
                            int acctNum = Integer.parseInt(pkg.substring(0, 6));
                            String serial = pkg.substring(8, 14);
                            if (!testpackageId(acctNum, serial) || !Util.validateCheckDigit(pkg)) {
                                System.out.println("That package does not exists." +
                                        " Please verify the information was entered correctly.");
                            } else
                                success = true;
                            accountantPrintPackage(acctNum, serial, "");
                        }
                    } while (!success);
                case "2":
                    int acctNum = selectCustomer("Which customer would you like to track packages of?");
                    if (acctNum == -3) {
                        userLogin();
                    } else if (acctNum == -4 || acctNum == -2) {
                        return;
                    } else {
                        ResultSet r = getPackagesByUser(acctNum);
                        try {
                            if (r.next()) {
                                do {
                                    String serial = r.getString("serial");
                                    String id = String.format("%06d%02d%s",
                                            acctNum, r.getInt("service_id_fk"), serial);
                                    String ID = Util.findCheckDigit(id);
                                    System.out.println("Package ID:" + ID);
                                    accountantPrintPackage(acctNum, serial, "\t");
                                } while (r.next());
                            }
                        } catch (SQLException | BadTrackingNumberFormatException e) {
                            e.printStackTrace();
                        }
                    }
                case "H":
                    acctHomePage();
                    break;
                case "B":
                    break;
                case "Q":
                    userLogin();
                    break;
            }
        } while(back);
    }

    /**
     * Returns all the packages a customer has set
     * @param acctNum The account number of the customer
     * @return A result set that conatins pacakge data
     */
    private ResultSet getPackagesByUser(int acctNum){
        String query = "Select * from package where account_number_fk=" + acctNum;
        return H2Access.createAndExecuteQuery(connection, query);
    }

    /**
     * Prints data about a package.
     * @param acctNum: The number of the account of the customer who ordered it
     * @param serial: The package serial
     * @param offset: Used for print formatting, an offset from the left margin (ie '/t')
     */
    private void accountantPrintPackage(int acctNum, String serial, String offset){
        ResultSet r = H2Access.trackPackage(acctNum, serial);
        try {
            if (r != null && r.next()) {
                System.out.println(offset + "   Date       Time       LocationID      City    State");
                do {
                    System.out.print(offset +r.getDate("date") + "\t");
                    System.out.print(r.getTime("time") + "\t");
                    System.out.print(r.getString("location_id_fk") + "\t");
                    System.out.print(r.getString("city") + "    ");
                    System.out.println(r.getString("state"));
                } while (r.next());
            } else
                System.out.println(offset + "No tracking information yet.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Lets the accounting employee edit the customer billing. They can add money to the
     * account when a customer sends them a check, or change how often a customer is billed.
     */
    private void editCustomerBilling(){
        Scanner in = new Scanner(System.in);
        int customerId = selectCustomer("Which customer would you like to edit the billing of?");
        if(customerId == -3){
            userLogin();
        } else if(customerId == -4 || customerId == -2){
            return;
        } else {
            basePage();
            System.out.println("[1] Add money to customer balance.");
            System.out.println("[2] Change customer billing period.");
            System.out.print("Please enter a choice: ");
            String userStr = getUserInput(new String[]{"1", "2", "H", "B", "Q"});
            switch (userStr) {
                case "H":
                    acctHomePage();
                    break;
                case "B":
                    break;
                case "Q":
                    userLogin();
                    break;
                case "1":
                    double amt;
                    do {
                        System.out.print("How much money should be added (0 to cancel): ");
                        amt = in.nextDouble();
                        if (amt < 0)
                            System.out.println("You can only add money to an account (enter 0 to cancel).");
                        else {
                            if(amt > 0) {
                                if (addMoney(amt, customerId)) {
                                    System.out.print("Money successfully added, new customer balance: ");
                                    System.out.printf("%.2f\n", getBalance(customerId));
                                } else {
                                    System.out.println("Could not add money to the account, contact a" +
                                            " systems administrator.");
                                }
                            }
                        }
                    } while (amt < 0);
                    break;
                case "2":
                    String newPeriod;
                    System.out.println("What should the new pay period be?");
                    System.out.println("[1] Annually");
                    System.out.println("[2] Bi-Annually");
                    System.out.println("[3] Quarterly");
                    System.out.println("[4] Monthly");
                    System.out.print("Choice: ");
                    int choice = Integer.parseInt(getUserInput(new String[]{"1", "2", "3", "4"}));
                    switch (choice) {
                        case 1:
                            newPeriod = "annually";
                            break;
                        case 2:
                            newPeriod = "bi-annually";
                            break;
                        case 3:
                            newPeriod = "quarterly";
                            break;
                        default:
                            newPeriod = "monthly";
                            break;
                    }
                    if (updateBillingPeriod(newPeriod, customerId))
                        System.out.println("Billing period successfully updated.");
                    else
                        System.out.println("Could not update the billing period.\n" +
                                "Contact a systems administrator.");
                    break;
            }
        }
    }

    /**
     * A method that lets the accounting employees add money to an account.
     * Also sets the employee_id on billing to this user, for accountability.
     * @param amount: The amount of money to add, must be more than 0
     * @param acctNum: The account number to add it to
     * @return If the action was successful.
     */
    private boolean addMoney(double amount, int acctNum){
        String query = "UPDATE billing SET balance_to_date = balance_to_date + %f WHERE id = %d";
        String lastTouchedQuery = "UPDATE billing SET employee_id = " + getId();

        H2Access.createAndExecute(connection, lastTouchedQuery);
        return H2Access.createAndExecute(connection, String.format(query, amount, acctNum));
    }

    /**
     * Lets an accounting employee update the customers billing period.
     * Also sets the employee_id on billing to this user, for accountability.
     * @param period: The new period, a string of annually, bi-annually, monthly, quarterly
     * @param acctNum: The account number to edit
     * @return If the query was successful.
     */
    private boolean updateBillingPeriod(String period, int acctNum){
        String query = "UPDATE billing SET pay_model = '%s' WHERE id = %d";
        String lastTouchedQuery = "UPDATE billing SET employee_id = " + getId();

        H2Access.createAndExecute(connection, lastTouchedQuery);
        return H2Access.createAndExecute(connection, String.format(query, period, acctNum));
    }

    /**
     * Lets the accounting employee select a customer. The account employee can select
     * customers one of three ways. First, they can provide a first and last name, second
     * they can give the account number associated with the customer (this will still
     * verify the account number is valid), third, they can give the username associated
     * with the customer. return Codes:
     *  -1  :   An error occurred
     *  -2  :   User wants to go back a page
     *  -3  :   User wants to quit
     *  -4  :   User wants to go to the home page
     * @param prompt: The situation specific prompt to ask to select a user
     * @return The account number of the selected customer, a different option, or an error
     */
    private int selectCustomer(String prompt){
        String desired = "";
        Scanner in = new Scanner(System.in);
        while(!desired.equals("Y")) {
            ResultSet r;
            String query = "";
            String method;
            boolean back;
            do {
                back = false;
                String choice;
                System.out.println("\n" + prompt);
                basePage();
                System.out.println("[1] First and Last name");
                System.out.println("[2] Account number");
                System.out.println("[3] Customer username");
                System.out.print("Please enter the number of who you'd like to select a customer: ");
                method = getUserInput(new String[]{"1", "2", "3", "4", "B", "H", "Q"});
                switch (method) {
                    case "1":
                        String[] name = new String[2];
                        do {
                            System.out.print("Please enter the First and Last name: ");
                            choice = in.nextLine();
                            if(choice.equalsIgnoreCase("B")) {
                                back = true;
                                break;
                            }else if(choice.equalsIgnoreCase("H")) {
                                return -4;
                            }
                            name = choice.split(" ");
                            if (name.length != 2)
                                System.out.println("Please enter exactly two names.");
                        } while (name.length != 2);
                        if(!back)
                            query = String.format("first_name='%s' AND last_name='%s'", name[0], name[1]);
                        break;
                    case "2":
                        System.out.print("Please enter the account number: ");
                        choice = in.nextLine();
                        if(choice.equalsIgnoreCase("B")) {
                            back = true;
                            break;
                        }else if(choice.equalsIgnoreCase("H")) {
                            return -4;
                        }
                        int acctNum = Integer.parseInt(choice);
                        query = "account_number=" + acctNum;
                        break;
                    case "3":
                        System.out.print("Please enter the user name: ");
                        choice = in.nextLine();
                        if(choice.equalsIgnoreCase("B")) {
                            back = true;
                            break;
                        }else if(choice.equalsIgnoreCase("H")) {
                            return -4;
                        }
                        String username = in.nextLine();
                        query = "account_number=" + getId(username);
                        break;
                    case "B":
                        return -2;
                    case "Q":
                        return -3;
                    case "H":
                        return -4;
                }
            } while(back);
            r = getCustomersWhere(query);
            try {
                if (!r.next())
                    System.out.println("No customer found matching that information. Please try again.");
                else {
                    printCustomer(r);
                    System.out.print("Is this the desired user? [Y/N]: ");
                    desired = getUserInput(new String[]{"Y", "N"});
                    if (desired.equalsIgnoreCase("Y")) {
                        r.beforeFirst();
                        if (r.next())
                            return r.getInt("account_number");
                    }
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return -1;
    }

    /**
     * Allows an accounting employee to generate and pay customer invoices.
     */
    private void generateCustomerInvoices(){
        int customerId = selectCustomer("Which customer would you like to generate an invoice for?");
        if(customerId == -3){
            userLogin();
        } else if(customerId == -4 || customerId == -2){
            return;
        } else {
            System.out.println("Generating Invoice...");

            int numCharges = 0;
            double amountDue = 0;
            double data[] = viewCharges(customerId);
            if (data != null) {
                numCharges = (int) data[0];
                amountDue = data[1];
            }

            System.out.printf("This user has %d unpaid charge(s) totalling $%.2f.\n", numCharges, amountDue);
            if (numCharges > 0) {
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

    }

    /**
     * Gets the balance of an account.
     * @param acct: Account number
     * @return double, the balance. Can be negative.
     */
    private double getBalance(int acct){
        double newBalance = Double.NaN;
        String getBalance = "SELECT balance_to_date FROM billing WHERE account_number_fk = " + acct;
        ResultSet currentBalance = H2Access.createAndExecuteQuery(connection, getBalance);
        try {
            if(currentBalance != null && currentBalance.next())
                newBalance = currentBalance.getDouble(1);
        } catch (SQLException e){
            e.printStackTrace();
        }
        return  newBalance;
    }

    /**
     * Allows an accounting employee to pay the charges for a customer.
     * @param acct: The account id of the customer who's paying
     * @param amountDue: The amount of money the customer owes for all thier unpaid packages.
     * @return The new balance of the account
     */
    private double payCharges(int acct, double amountDue){
        double newBalance = getBalance(acct);
        if(!Double.isNaN(newBalance)){
            newBalance -= amountDue;
            String payChargeQuery = "UPDATE charge SET paid = 1 where account_number_fk = " + acct;
            String editBalanceQuery = "UPDATE billing SET balance_to_date = %f WHERE account_number_fk = %d";
            String lastTouchedQuery = "UPDATE billing SET employee_id = " + getId();

            H2Access.createAndExecute(connection, lastTouchedQuery);
            H2Access.createAndExecute(connection, payChargeQuery);
            H2Access.createAndExecute(connection, String.format(editBalanceQuery, newBalance, acct));
        }
        return newBalance;
    }

    /**
     * Lets an accountant view money much money this customer ows for thier packages
     * @param accountID: The id of the account to look at
     * @return The amount of money owed
     */
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

    public static void userLogin(){
        Scanner in = new Scanner(System.in);
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
                    System.out.println("Congratulations! You've been logged in as a(n) " +
                            employeeType.replace("_", " ") + ".");
                    // Package employee view
                    if(employeeType.equalsIgnoreCase("package_employee")){
                        boolean idSuccess;
                        do {
                            // Sample number: VGLJPA1YKP5G
                            System.out.print("Please enter your location ID (or Q to quit): ");
                            String locationID = in.nextLine();
                            if(locationID.equalsIgnoreCase("Q")) {
                                System.out.println("Have A good day!\n");
                                break;
                            }
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
                                employee.close();
                                System.out.println("Have A good day!\n");
                            }
                        }
                        while (!idSuccess);
                    } else if(employeeType.equalsIgnoreCase("accounting_employee")){
                        employee.acctHomePage();
                        employee.close();
                        System.out.println("Have A good day!\n");
                    }
                }
            } catch (SQLException e){
                System.out.println("Could not log in with those credentials. " +
                        "Check your username and password. " +
                        "If the issue persists, contact an administrator.");
            }
        } while(true);
    }

    /**
     * The main method to let employees log in and do work
     * @param args: No arguments needed
     */
    public static void main(String[] args){
        userLogin();
    }
}
