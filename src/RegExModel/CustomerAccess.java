package RegExModel;
import java.sql.*;
import java.util.Random;

/**
 * Created by Walter Schaertl on 3/24/2019.
 * @author Walter Schaertl, s b binder,
 */

/**
 * Class that contains ease of use methods for the customers that use the system.
 * Functions here are called by the server.
 */
public class CustomerAccess implements AutoCloseable{
    private String username;
    private Connection connection;
    private H2Access h2;

    /**
     * How an customer logs in
     * @param username the customer's username
     * @param password the customer's password
     * @throws SQLException If the username/password is incorrect, an exception is raised
     */
    public CustomerAccess(String username, String password) throws SQLException {
        this.username = username;
        this.h2 = new H2Access();
        this.connection = H2Access.createConnection(username, password);
    }

    /**
     * Sets up a new billing situation for a customer. By default, the balance starts at zero,
     * and the pay model is monthly, and the employee foreign key is null. If this user
     * become prolific or wants to change values, they call in, an accounting employee will
     * be assigned to them, and thet employee will update their billing.
     * @return boolean true if the billing was set up correctly
     */
    public boolean setUpBillingInfo() {
        // Zero balance, monthly payments, account number, no employee assigned
        String billingQuery = "INSERT INTO billing(balance_to_date, pay_model, " +
                "account_number_fk, employee_id) VALUES(0, 'monthly', %d, null);";
        // Query to update the billing_fk of the customer
        String customerQuery = "UPDATE customer SET billing_fk=%d WHERE account_number=%d";
        // Get the account number fk (which is the general fk of the user)
        int accountNumber = H2Access.getUserFK(username);
        // The billing ID, to be associated with the customer once it's created
        int billingID = 0;
        try {
            // While this function sets up customer billing, the database access is not
            // done with a customer access
            Connection conn = H2Access.createConnection("me", "password");
            String formattedQuery = String.format(billingQuery, accountNumber);
            PreparedStatement prep = conn.prepareStatement(formattedQuery, Statement.RETURN_GENERATED_KEYS);
            prep.executeUpdate();
            ResultSet keys = prep.getGeneratedKeys();
            if (keys.next())
                billingID = keys.getInt("ID");

            // Update the customer with the fk to this new billing object
            formattedQuery = String.format(customerQuery, billingID, accountNumber);
            conn.createStatement().execute(formattedQuery);
        } catch (SQLException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Allows the user to enter or update their basic information.
     *
     * @param firstName The customer's first name. Null to keep the same.
     * @param lastName The customer's last name. Null to keep the same
     * @param phoneNumber The customer's phone number. Null to keep the same
     * @return a boolean, true if the update was successful, else false.
     */
    public boolean changeBasicInformation(String firstName, String lastName, String phoneNumber){
        String fnQuery = firstName != null ? "first_name='" + firstName + "', " : "";
        String lnQuery = lastName != null ? "last_name='" + lastName + "', " : "";
        String phoneNum = phoneNumber != null ? "phone_no='" + phoneNumber + "'" : "";
        int userFK = H2Access.getUserFK(username);
        if(firstName == null && lastName == null && phoneNumber == null)
            return true;
        if(phoneNumber == null && lastName != null)
            lnQuery = lnQuery.substring(0, lnQuery.length() - 2);
        if(lastName == null && phoneNumber == null)
            fnQuery = fnQuery.substring(0, fnQuery.length() - 2);

        String query = String.format("UPDATE customer SET %s %s %s WHERE account_number=%s",
                fnQuery, lnQuery, phoneNum, userFK);
        return H2Access.createAndExecute(connection, query);
    }

    /**
     * Lets the User enter in minimal information about their address. All foreign keys
     * are generated/looked up in the process. If not applicable, strings should be blank, not null
     * Error codes:
     *  0   Success
     *  -1  Could not get user general purpose foreignKey
     *  -2  ZipCode doesn't exists
     *  -3  An unexpected SQL exception occurred.
     * @param company The company name
     * @param attention The name of the individual
     * @param streetLineOne The first address line
     * @param streetLineTwo The second address line
     * @param zipCode The 5 digit zip code
     * @return an error code, 0 for success
     */
    public int enterAddress(String company, String attention, String streetLineOne,
                            String streetLineTwo, String zipCode){
        String zipQuery = "SELECT id FROM zip_code WHERE zip_code = " + zipCode + ";";
        String addressQuery = "INSERT INTO " +
                "address(company, attn, street_line_1, street_line_2, zip_fk, account_number_fk) " +
                "VALUES('%s', '%s', '%s', '%s', %d, %d);";
        String customerQuery = "UPDATE customer SET mailing_address_id_fk=%d WHERE account_number=%d";
        ResultSet r;
        int zipFK;
        int addressID = 0;

        // Get the user's fk
        int userFK = H2Access.getUserFK(username);
        if(userFK == -1)
            return -1;

        try {
            // Get the ZIP ID fk based on the zipCode
            r = connection.createStatement().executeQuery(zipQuery);
            if(!r.next())
                return -2;
            else
                zipFK = r.getInt(1);

            // Create the entry in the address table
            addressQuery = String.format(addressQuery, company, attention,
                    streetLineOne, streetLineTwo, zipFK, userFK);
            PreparedStatement prep = connection.prepareStatement(addressQuery, Statement.RETURN_GENERATED_KEYS);
            prep.executeUpdate();
            ResultSet keys = prep.getGeneratedKeys();
            if (keys.next())
                addressID = keys.getInt("ID");

            // Set this Id as the users main address
            connection.createStatement().execute(String.format(customerQuery, addressID, userFK));
            return 0;
        } catch (SQLException e){e.printStackTrace();}
        return -3;
    }

    /**
     * Call this one when adding an address; any address; will look to see if it exists first,
     * then return any existing one or a new address of that type.
     * @param company
     * @param attention
     * @param streetLine1
     * @param streetLine2
     * @param zip_ID_fk
     * @param account_number_fk
     * @return
     * @throws SQLException
     */
    public ResultSet createNewAddress(String company, String attention, String streetLine1, String streetLine2,
                                      String zip_ID_fk, String account_number_fk) throws SQLException {

        ResultSet does_it_exist = retrieveAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
        if (!does_it_exist.next()){ // this is how you check if its an empty ResultSet, per stack overflow
            createAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
            does_it_exist = retrieveAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
        }

        return does_it_exist;
    }

    /**
     * Small method to return basic user information.
     *
     * @return  A ResultSet containing the basic user information.
     */
    public ResultSet getUserInformation() {
        // queries from the user table to get this user's information
        return H2Access.createAndExecuteQuery(
            this.connection,
            "SELECT username, customer.*, address.*, zip_code.zip_code, zip_code.city, zip_code.state " +
            "FROM user " +
            "INNER JOIN customer ON general_fk=customer.account_number " +
            "INNER JOIN address ON customer.mailing_address_ID_fk=address.ID " +
            "INNER JOIN zip_code ON address.zip_fk=zip_code.id " +
            "WHERE username='" + this.username + "';"
        );
    }

    public ResultSet getSentPackages() {
        // queries the packages table to get a list of all the packages this user has sent
        int userFK = H2Access.getUserFK(this.username);

        // returns all of the packages
        return H2Access.createAndExecuteQuery(
            this.connection,
            "SELECT account_number_fk, service_id_fk, serial " +
            "FROM package " +
            "WHERE account_number_fk='" + userFK + "';"
        );
    }

    /**
     * helper function for @function createNewAddress
     * @param company
     * @param attention
     * @param streetLine1
     * @param streetLine2
     * @param zip_ID_fk
     * @param account_number_fk
     * @return
     */
    private ResultSet retrieveAddress(String company, String attention, String streetLine1, String streetLine2,
                                      String zip_ID_fk, String account_number_fk){
        int zip_ID_fk_numeric = Integer.parseInt(zip_ID_fk);
        int account_number_fk_numeric = Integer.parseInt(account_number_fk);
        String query =
                "SELECT * FROM address "+
                "WHERE (COMPANY = '" + company + "') AND " +
                "(ATTN = '" + attention +"') AND " +
                "(STREET_LINE_1 = '" + streetLine1 + "') AND " +
                "(STREET_LINE_2 = '" + streetLine2 + "') AND " +
                "(ZIP_FK = "+ zip_ID_fk_numeric + ") AND " +
                "(ACCOUNT_NUMBER_FK = " + account_number_fk_numeric + ");";
        return h2.createAndExecuteQuery(connection, query);
    }
    /**
     * helper function for createNewAddress
     * adds new address for a customer. Called after querying whether this address already exists.
     * @param company
     * @param attention
     * @param streetLine1
     * @param streetLine2
     * @param zip_ID_fk
     * @param account_number_fk
     * @return the result of the query, which should be empty.
     */
    private void createAddress(String company, String attention, String streetLine1, String streetLine2,
                              String zip_ID_fk, String account_number_fk){
        int zip_ID_fk_numeric = Integer.parseInt(zip_ID_fk);
        int account_number_fk_numeric = Integer.parseInt(account_number_fk);
        String query =
                "INSERT INTO address "+
                "(COMPANY, ATTN, STREET_LINE_1, STREET_LINE_2, ZIP_FK, ACCOUNT_NUMBER_FK)" +
                " VALUES(" +
                "'" + company + "', " +
                "'" + attention + "', " +
                "'" + streetLine1 + "', " +
                "'" + streetLine2 + "', " +
                "'" + zip_ID_fk_numeric + "', " +
                "'" + account_number_fk_numeric + "');";

        H2Access.createAndExecute(connection, query);
    }

    /**
     * Gets all addresses in customer's "rolodex", that they've ever sent packages from or to
     * @param account_number_fk
     * @return ResultSet of all addresses with their account number associated
     */
    public ResultSet getAllAddresses(String account_number_fk){
        int account_number_fk_numeric = Integer.parseInt(account_number_fk);
        String query = "SELECT * FROM address " + "WHERE (ACCOUNT_NUMBER_FK = " + account_number_fk_numeric + ");";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * Sets the customer's 'home' address from the address they've used
     * Return code
     *  0   Success
     *  1   The address_id provided does not exist or does not belong to this user
     *  2   The home address could not be updated to key of the new address
     *  3   Unknown error
     *
     *  @param addressId The ID of the address to set at the 'home' address
     *  @return an integer status code
     */
    public int setHomeAddress(int addressId){
        int accountNumber = H2Access.getUserFK(username);
        String existsQuery = "SELECT account_number_fk FROM address WHERE id=" + addressId;
        String setQuery = "UPDATE customer SET mailing_address_id_fk=%d WHERE account_number=%d";
        try{
            ResultSet r = H2Access.createAndExecuteQuery(connection, existsQuery);
            if(r.next())
                if(r.getInt(1) != accountNumber)
                    return 1;
            else
                return 1;
            String formattedSetQuery = String.format(setQuery, addressId, accountNumber);
            if(H2Access.createAndExecute(connection, formattedSetQuery))
                return 0;
        } catch (SQLException e){
            e.printStackTrace();
            return 3;
        }
        return 2;
    }

    /**
     *
     * @param city
     * @param state
     * @return ResultSet of zips for that city/state combo
     */
    public ResultSet zipCodeLookupByCityState(String city, String state){
        String query =
                "SELECT * FROM ZIP_CODE WHERE (CITY = '" + city + "') AND (STATE ='" + state + "');";
        return h2.createAndExecuteQuery(connection, query);
    }

    /*
        FULLY UI FACING - invoke from server
        Before inserting:
            -looks up zip codes for destination, origin
                if not found, uses first appropriate city/state match (or throw exception if no city/state match)
            -looks up OR creates addresses for destination, origin
        After inserting:
            -create charge record
            -update billing
        returns createPackage after those have been done

        Does *NOT* create initial transaction - that needs to be done by a package employee
     */
    public ResultSet sendPackage(String account_number_fk, String service_id_fk, String dim_height, String dim_length,
                                 String dim_depth, String weight,
                                 String origin_company, String origin_attention,  String origin_street1,
                                 String origin_street2, String origin_city, String origin_state, String origin_zip,
                                 String destination_company, String destination_attention, String destination_street1,
                                 String destination_street2, String destination_city, String destination_state,
                                 String destination_zip) throws SQLException{
        ResultSet o_zip = zipCodeLookupByCityState(origin_city, origin_state);
        ResultSet d_zip = zipCodeLookupByCityState(destination_city, destination_state);
        int o_zip_numeric = Integer.parseInt(origin_zip);
        int d_zip_numeric = Integer.parseInt(destination_zip);
        String origin_has_addr = "none";
        String dest_has_addr = "none";

        // detect zip code IDs for origin & destination
        /* edit: nope
        do {
            if (o_zip.getInt(1) == o_zip_numeric ){
                origin_has_addr = o_zip.getString(2);
            }
        } while (o_zip.next());
        if(origin_has_addr.equals("none")){
            o_zip.first();
            origin_has_addr = o_zip.getString(2);
        }
        do {
            if (d_zip.getInt(1) == d_zip_numeric ){
                dest_has_addr = d_zip.getString(2);
            }
        } while (d_zip.next());
        if(dest_has_addr.equals("none")){
            d_zip.first();
            dest_has_addr = d_zip.getString(2);
        } */

        if(o_zip.next()){
            origin_has_addr = o_zip.getString("ID");
        } else {
            o_zip = h2.createAndExecuteQuery(connection, "SELECT ID FROM ZIP_CODE WHERE ZIP_CODE = " + Integer.parseInt(origin_zip));
            o_zip.next();
            origin_has_addr = o_zip.getString("ID");
        }
        if(d_zip.next()){
            // wet code, don't care.
            dest_has_addr = d_zip.getString("ID");
        } else {
            d_zip = h2.createAndExecuteQuery(connection, "SELECT ID FROM ZIP_CODE WHERE ZIP_CODE = " + Integer.parseInt(destination_zip));
            d_zip.next();
            dest_has_addr = d_zip.getString("ID");
        }


        // get address fks
        ResultSet origin_RS = createNewAddress(origin_company, origin_attention, origin_street1, origin_street2,
                origin_has_addr, account_number_fk);
        ResultSet destination_RS = createNewAddress(destination_company, destination_attention, destination_street1,
                destination_street2, dest_has_addr, account_number_fk);

        // get location fks
        String origin_id = find_location(origin_has_addr, 'O');
        String destination_id = find_location(dest_has_addr, 'D');

        ResultSet pkg = createPackage(account_number_fk, service_id_fk, dim_height, dim_length, dim_depth, weight, origin_id, destination_id);
        // ADD CHARGES

        ResultSet rates = getCustomerRates(account_number_fk);
        int billable_weight;
        int _h = Integer.parseInt(dim_height);
        int _l = Integer.parseInt(dim_length);
        int _d = Integer.parseInt(dim_depth);
        int _w = Integer.parseInt(weight);
        int dim_break = rates.getInt("dim_rating_break");

        billable_weight = _h * _l * _d;
        billable_weight /= dim_break;

        if(_w > billable_weight){
            billable_weight = _w;
        }

        ResultSet charges = createCharges(rates, account_number_fk, pkg.getString(2), billable_weight, service_id_fk);
        // charges should be the result of an insert query, so empty;

        // call billing update method
        ResultSet billing = updateBillingAmountDue(account_number_fk);
        // also empty - result of update query

        return pkg;
    }

    /**
     * updates billing amount due
     * @param acct
     * @return
     * @throws SQLException
     */
    private ResultSet updateBillingAmountDue(String acct) throws SQLException{
        // get current outstanding charges (from charge)
        // & get total paid (from charge)
        String Qoutstanding = "SELECT SUM(price) FROM CHARGE WHERE account_number_fk = '" + acct + "' AND paid = 0;";
        ResultSet outstanding = h2.createAndExecuteQuery(connection, Qoutstanding);
        double due = outstanding.getDouble(1);
        String QcurrentBal = "SELECT balance_to_date FROM billing WHERE account_number_fk = '" + acct + "';";
        ResultSet currentBalance = h2.createAndExecuteQuery(connection, QcurrentBal);
        due += currentBalance.getDouble(1);
        // update billing table
        String QupdateOutstanding = "UPDATE billing SET balance_to_date = " + due + " WHERE account_number_fk = '" + acct + "';";
        return h2.createAndExecuteQuery(connection, QupdateOutstanding);



    }

    /**
     * Calculates charges based on rates, billable weight, service.
     * Adds record in charge table
     * returns empty ResultSet
     * @param rate
     * @param account_number_fk
     * @param serial
     * @param billable_weight
     * @param service_id
     * @return
     * @throws SQLException
     */
    private ResultSet createCharges(ResultSet rate, String account_number_fk, String serial,
                                    int billable_weight, String service_id) throws SQLException{
        int service = Integer.parseInt(service_id);
        double svc_multiplier = 1.0;
        int priority = service / 4;
        int hazardous = (service / 2) % 2;
        int signature = (service % 2);
        double base;
        double rush;
        if(signature == 0){
            svc_multiplier += .05;
        }
        if(hazardous == 1){
            svc_multiplier += .15;
        }
        if(priority > 2){
            base = rate.getDouble(2);
        } else {
            base = rate.getDouble(1);
        }
        if(1 == priority % 1){
            rush = rate.getDouble(3);
        } else {
            rush = 1.0;
        }
        double totalprice = ((double)billable_weight)*base*rush*svc_multiplier;
        String QInsert = "INSERT INTO CHARGE (PRICE, ACCOUNT_NUMBER_FK, PACKAGE_SERIAL_FK, SERVICE_ID, PAID) " +
                            "VALUES(" +
                            totalprice + ", " +
                            account_number_fk + ", '" +
                            serial + "', " +
                            service_id + ", " +
                            "0 );";
        return h2.createAndExecuteQuery(connection, QInsert);

    }

    /**
     * gets customer's current negotiated rates.
     * @param account_number
     * @return
     * @throws SQLException
     */
    public ResultSet getCustomerRates(String account_number) throws SQLException{
        String QrateID = "SELECT negotiated_rate_ID_fk FROM CUSTOMER WHERE account_number = '" + account_number + "';";
        ResultSet rateID = h2.createAndExecuteQuery(connection, QrateID);
        int rate_fk = rateID.getInt("negotiated_rate_ID_fk");
        String QGetRates = "SELECT * FROM RATE WHERE negotiated_rate_ID = " + rate_fk + ";";
        ResultSet rates = h2.createAndExecuteQuery(connection, QGetRates);
        return rates;

    }

    /**
     * helper method for sendPackage
     * Creates package serial.
     * @param account_number_fk
     * @param service_id_fk
     * @param dim_height
     * @param dim_length
     * @param dim_depth
     * @param weight
     * @param origin
     * @param destination
     * @return
     */
    private ResultSet createPackage(String account_number_fk, String service_id_fk, String dim_height, String dim_length,
                                    String dim_depth, String weight, String origin, String destination){

        String getLast = "SELECT MAX(serial) FROM package WHERE account_number_fk = '" + account_number_fk + "';";

        ResultSet last = h2.createAndExecuteQuery(connection, getLast);
        // GET the last serial this customer has sent, add 1
        String lastSerial;
        char nextSerial[] = new char[6];
        boolean carry = false;


        try {
            lastSerial = last.getString(0);
        } catch (SQLException e) {
            lastSerial = "AAAAAA";
        }

        int i = 5;
        if (lastSerial.charAt(i) == 'Z') {
            nextSerial[i] = '0';

        } else if ( lastSerial.charAt(i) == '9' ){
            nextSerial[i] = 'A';
            carry = true;

        } else {
            nextSerial[i] = (char) (lastSerial.charAt(i) + 1);
        }
        if(!carry){
            for(int j = 4; j < 0; j--){
                nextSerial[j] = (char) (lastSerial.charAt(i) + 1);
            }
        }
        while (carry){
            // this will work fine until someone mails their
            // 2176782336th package ( little over 2billion )
            // so i guess we're not aiming to work with amazon

            i--;
            if (lastSerial.charAt(i) == 'Z') {
                nextSerial[i] = '0';
                carry = false;

            } else if ( lastSerial.charAt(i) == '9' ){
                nextSerial[i] = 'A';
                carry = true;

            } else {
                nextSerial[i] = (char) (lastSerial.charAt(i) + 1);
                carry = false;
            }
        }


        String query =  "INSERT INTO PACKAGE " +
                        "(ACCOUNT_NUMBER_FK, SERVICE_ID_FK, SERIAL, HEIGHT, LENGTH, DEPTH, WEIGHT, ORIGIN_FK, DESTINATION_FK)" +
                        " VALUES (" +
                        account_number_fk + ", " +
                        service_id_fk + ", " +
                        "'" + nextSerial.toString() + "', " +
                        dim_height + ", " +
                        dim_length + ", " +
                        dim_depth + ", " +
                        weight + ", " +
                        "'" + origin + "', " +
                        "'" + destination + "');";
        ResultSet ins_new_rec = h2.createAndExecuteQuery(connection, query);
        String query2 = "SELECT * FROM PACKAGE" +
                        " WHERE (ACCOUNT_NUMBER_FK = " + account_number_fk +
                        ") AND (SERIAL = '" + nextSerial.toString() + "');";

        ins_new_rec = h2.createAndExecuteQuery(connection, query2);
        return ins_new_rec;

    }

    /**
     * helper method for finding location strings based on address ID + whether is destination
     * or origin
     * @param address_id_fk
     * @param constraint
     * @return location string
     * @throws SQLException
     */
    // Use capital letters; not set up to handle lower case
    private String find_location(String address_id_fk, char constraint) throws SQLException {
        String query = "SELECT * FROM LOCATION WHERE ADDRESS_ID = " + address_id_fk + ";";
        ResultSet matches = H2Access.createAndExecuteQuery(connection, query);
        // new changes
        if( ((constraint == 'O')||(constraint == 'D')||(constraint == 'o')||(constraint == 'd'))&&(matches.next())) {
            do {

                switch (constraint) {
                    case 'O':
                    case 'o':
                        if (matches.getString(1).charAt(1) == 'O') {
                            return matches.getString(1);
                        }
                        break;
                    case 'D':
                    case 'd':
                        if (matches.getString(1).charAt(1) == 'D') {
                            return matches.getString(1);
                        }
                        break;
                    default:
                        // Vehicle & Hub cases here, I guess?
                        // they shouldn't work, regardless - no address ID fk on those entries
                        break;

                }
            } while (matches.next());
        }

        // CASE: origin/destination not found
        //      create new ones
        // CASE: 'H' / 'V' - create new
        Random r = new Random();
        String new_Location_ID;
        String query2;
        ResultSet ok;
        char[] ch_to_array = new char[12];
        if((constraint == 'O')||(constraint == 'D')){
            ch_to_array[0] = 'T';
            ch_to_array[1] = constraint;
        } else {
            ch_to_array[0] = constraint;
            ch_to_array[1] = (char)(r.nextInt(10) + '0');
        }
        do {
            ch_to_array[2] = (char)(r.nextInt(26) + 'A');
            ch_to_array[3] = (char)(r.nextInt(26) + 'A');
            ch_to_array[4] = (char)(r.nextInt(26) + 'A');
            ch_to_array[5] = (char)(r.nextInt(26) + 'A');
            ch_to_array[6] = (char)(r.nextInt(26) + 'A');
            ch_to_array[7] = (char)(r.nextInt(26) + 'A');
            ch_to_array[8] = (char)(r.nextInt(26) + 'A');
            ch_to_array[9] = (char)(r.nextInt(26) + 'A');
            ch_to_array[10] = (char)(r.nextInt(26) + 'A');
            ch_to_array[11] = (char)(r.nextInt(26) + 'A');
            query2 = "SELECT * FROM LOCATION WHERE ID = '" + String.copyValueOf(ch_to_array) + "';";
            ok = h2.createAndExecuteQuery(connection, query2);

        } while (ok.next());
        return String.copyValueOf(ch_to_array);

    }

    /**
     * Function required to be implemented by AutoCloseable.
     * Lets CustomerAccess be used in a 'try with resources' block
     */
    @Override
    public void close(){
        h2.closeConnection(this.connection);
    }

    public ResultSet viewAccount(String account_number){
        String query = "(SELECT * FROM Customer WHERE account_number = '" + account_number + "') UNION " +
                        "(SELECT * FROM Billing WHERE account_number_fk = '" + account_number + "');";
        return h2.createAndExecuteQuery(connection, query);



    }
}
