package RegExModel;

// FILE: CustomerAccess.java

import java.sql.*;
import java.util.Random;

/**
 * Class of ease-of-use methods for the customer view to use.
 *
 * @author Walter Schaertl, Sam Binder, Kevin J. Becker (kjb2503)
 * @version 3/24/2019
 */
public class CustomerAccess implements AutoCloseable {
    /**
     * The Customer's username
     */
    private String username;

    /**
     * The Connection to the server.
     */
    private Connection connection;

    /**
     * Constructor which serves as a "login" activity as well.
     *
     * @param username The Customer's username.
     * @param password The Customer's password.
     * @throws SQLException If the username/password combination is wrong, a SQLException will be thrown.
     */
    public CustomerAccess(String username, String password) throws SQLException {
        // sets the username in place
        this.username = username;
        // attempts to create a connection to the DB
        this.connection = H2Access.createConnection(username, password);
    }

    /**
     * Sets up new billing information for a customer.  By default, their balance starts at 0 and the
     * pay model is monthly.  Employee foreign key will be null at the start, too.
     * <p>
     * If the user wants to get custom billing items, they will call an accounting employee to get them set up with any billing customization.
     *
     * @return True if the billing queries were run to completion; false otherwise.
     */
    public boolean setUpBillingInfo() {
        // creates an initial billing query to set the defaults
        String billingQuery = "INSERT INTO billing(balance_to_date, pay_model, " +
                "account_number_fk, employee_id) VALUES(0, 'monthly', %d, null);";

        // updates a customer with a billing_fk once that has been created
        String customerQuery = "UPDATE customer SET billing_fk=%d WHERE account_number=%d";


        // gets the account number of the user with this username
        int accountNumber = H2Access.getUserFK(this.username);

        // this will be updated with the billing ID for the customer
        int billingID = 0;

        try {
            // creates a connection to the DB
            Connection conn = H2Access.createConnection("me", "password");

            // formats the billing query with the account number
            String formattedQuery = String.format(billingQuery, accountNumber);

            // creates a prepared statement with the formatted query
            PreparedStatement prep = conn.prepareStatement(
                    formattedQuery,
                    Statement.RETURN_GENERATED_KEYS
            );
            // executes the query
            prep.executeUpdate();

            // gets a key ResultSet based on the prepared statement
            ResultSet keys = prep.getGeneratedKeys();

            // if there is a next key
            if (keys.next()) {
                // extracts the ID
                billingID = keys.getInt("ID");
            }

            // formats the customer update query with th enew billing ID
            formattedQuery = String.format(customerQuery, billingID, accountNumber);
            // creates another statement and executes the update
            conn.createStatement().execute(formattedQuery);
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
        // if we make it to here, the method succeeded, true can be returned
        return true;
    }

    /**
     * Updates a user's basic information.
     *
     * @param firstName   The customer's first name. Null to not update it.
     * @param lastName    The customer's last name. Null to not update it.
     * @param phoneNumber The customer's phone number. Null to not update it.
     *
     * @return a boolean, true if the update was successful, else false.
     */
    public boolean changeBasicInformation(String firstName, String lastName, String phoneNumber) {
        // the first name section of the update query
        String fnQuery = firstName != null ? "first_name='" + firstName + "', " : "";
        // the last name section of the query
        String lnQuery = lastName != null ? "last_name='" + lastName + "', " : "";
        // the phone number section of the query
        String phoneNumQuery = phoneNumber != null ? "phone_no='" + phoneNumber + "'" : "";

        // gets the account number from the database
        int accountNumber = H2Access.getUserFK(username);

        // if all of the parameters are null, return true
        if (firstName == null && lastName == null && phoneNumber == null) {
            return true;
        }

        // if phoneNumber parameter is null but last name is not
        if (phoneNumber == null && lastName != null) {
            // strip the comma from the last name query
            lnQuery = lnQuery.substring(0, lnQuery.length() - 2);
        }

        // if the lastName and phoneNumber parameters are null
        if (lastName == null && phoneNumber == null) {
            // strip the comma from the first name query
            fnQuery = fnQuery.substring(0, fnQuery.length() - 2);
        }

        // builds the query to update the user
        String query = String.format(
                "UPDATE customer SET %s %s %s WHERE account_number=%s",
                fnQuery,
                lnQuery,
                phoneNumQuery,
                accountNumber
        );

        // returns the result of the query execution
        return H2Access.createAndExecute(this.connection, query);
    }

    /**
     * Lets the User enter in minimal information about their address. All foreign keys
     * are generated/looked up in the process.
     *
     * If not applicable, strings should be BLANK, not null.
     *
     * Error codes:
     *  0  - success
     * -1  - could not get user general purpose foreign key
     * -2  - zip code doesn't exists
     * -3  - an unexpected SQL exception occurred.
     *
     * @param company       The company name.
     * @param attention     The name of the individual.
     * @param streetLineOne The first address line.
     * @param streetLineTwo The second address line.
     * @param zipCode       The 5 digit zip code.
     *
     * @return An integer error code; 0 means success.
     */
    public int enterAddress(String company,
                            String attention,
                            String streetLineOne,
                            String streetLineTwo,
                            String zipCode) {
        // zip code selection query
        String zipQuery = "SELECT id FROM zip_code WHERE zip_code = " + zipCode + ";";
        // address insertion query
        String addressQuery = "INSERT INTO " +
                "address (company, attn, street_line_1, street_line_2, zip_fk, account_number_fk) " +
                "VALUES('%s', '%s', '%s', '%s', %d, %d);";

        // customer update query
        String customerQuery = "UPDATE customer SET mailing_address_id_fk=%d WHERE account_number=%d";

        // gets the account number of the user
        int accountNumber = H2Access.getUserFK(username);

        // if the account number is -1 there was an issue, return it
        if (accountNumber == -1) {
            return -1;
        }

        // try to run all of the other queries
        try {
            // get the zip code ID
            ResultSet res = this.connection.createStatement().executeQuery(zipQuery);
            // the zip code ID and the address ID
            int zipCodeID, addressID;

            // if there was no zip code matching in the table
            if (!res.next()) {
                return -2;
            }
            // else extract the zip code ID
            else {
                zipCodeID = res.getInt(1);
            }

            // formats the address insertion query
            addressQuery = String.format(
                    addressQuery,
                    company,
                    attention,
                    streetLineOne,
                    streetLineTwo,
                    zipCodeID,
                    accountNumber
            );

            // creates a new prepared statement with our address insertion query
            PreparedStatement prep = this.connection.prepareStatement(
                    addressQuery,
                    Statement.RETURN_GENERATED_KEYS
            );
            // executes the query
            prep.executeUpdate();

            // gets the generated keys from the prepared statement
            ResultSet keys = prep.getGeneratedKeys();

            // if the result had a value
            if (keys.next()) {
                addressID = keys.getInt("ID");
            } else {
                return -3;
            }

            // Set this Id as the users main address
            this.connection.createStatement().execute(
                String.format(
                    customerQuery,
                    addressID,
                    accountNumber
                )
            );

            // once we get here, return a 0
            return 0;
        } catch (SQLException e) {
            // catching a SQLException will return a -3
            e.printStackTrace();
            return -3;
        }
    }

    /**
     * This method is used when adding an address to the address table.  This will return the
     * address row that was inserted, if it didn't exist already, or the row of the address that
     * already exists.
     *
     * @param company  The company that this address is for.
     * @param attention  The attention line that this address is for.
     * @param addressLine1  The address line 1 for this address.
     * @param addressLine2  The address line 2 for this address.
     * @param zipID The ID of the zip code.
     * @param accountNumber  The account number creating this address.
     *
     * @return  A ResultSet containing the address row.
     *
     * @throws SQLException  If any SQLException is encountered, it is thrown to the caller.
     */
    public ResultSet createNewAddress(String company,
                                      String attention,
                                      String addressLine1,
                                      String addressLine2,
                                      String zipID,
                                      String accountNumber) throws SQLException {
        // sees if the address exists in the database already
        ResultSet addressRetrieval = retrieveAddress(
            company,
            attention,
            addressLine1,
            addressLine2,
            zipID,
            accountNumber
        );

        // if there wasn't an address already in teh table
        if (!addressRetrieval.next()) {
            // create the address
            createAddress(
                company,
                attention,
                addressLine1,
                addressLine2,
                zipID,
                accountNumber
            );

            // retrieve the address that was just put in
            addressRetrieval = retrieveAddress(
                company,
                attention,
                addressLine1,
                addressLine2,
                zipID,
                accountNumber
            );
        }

        // returns the address result
        return addressRetrieval;
    }

    /**
     * A method to get basic user information.
     *
     * @return A ResultSet containing the basic user information.
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

    /**
     * A method to get packages sent by the user.
     *
     * @return  All packages sent by the current user.
     */
    public ResultSet getSentPackages() {
        // queries the packages table to get a list of all the packages this user has sent
        int accountNumber = H2Access.getUserFK(this.username);

        // returns all of the packages
        return H2Access.createAndExecuteQuery(
                this.connection,
                "SELECT account_number_fk, service_id_fk, serial " +
                        "FROM package " +
                        "WHERE account_number_fk='" + accountNumber + "';"
        );
    }

    /**
     * A method to retrieve an address from the database.
     *
     * @param company  The company that this address is for.
     * @param attention  The attention line that this address is for.
     * @param addressLine1  The address line 1 for this address.
     * @param addressLine2  The address line 2 for this address.
     * @param zipID The ID of the zip code.
     * @param accountNumber  The account number creating this address.
     *
     * @return  The address row from the database.
     */
    private ResultSet retrieveAddress(String company,
                                      String attention,
                                      String addressLine1,
                                      String addressLine2,
                                      String zipID,
                                      String accountNumber) {
        String query =
            "SELECT * FROM address " +
            "WHERE (COMPANY = '" + company + "') AND " +
            "(ATTN = '" + attention + "') AND " +
            "(STREET_LINE_1 = '" + addressLine1 + "') AND " +
            "(STREET_LINE_2 = '" + addressLine2 + "') AND " +
            "(ZIP_FK = " + zipID + ") AND " +
            "(ACCOUNT_NUMBER_FK = " + accountNumber + ");";

        // returns the result of the select query
        return H2Access.createAndExecuteQuery(this.connection, query);
    }

    /**
     * A helper method to create an address in the database.
     *
     * @param company  The company that this address is for.
     * @param attention  The attention line that this address is for.
     * @param addressLine1  The address line 1 for this address.
     * @param addressLine2  The address line 2 for this address.
     * @param zipID The ID of the zip code.
     * @param accountNumber  The account number creating this address.
     */
    private void createAddress(String company,
                               String attention,
                               String addressLine1,
                               String addressLine2,
                               String zipID,
                               String accountNumber) {
        // builds the insertion query
        String query =
            "INSERT INTO address " +
            "(COMPANY, ATTN, STREET_LINE_1, STREET_LINE_2, ZIP_FK, ACCOUNT_NUMBER_FK)" +
            " VALUES(" +
            "'" + company + "', " +
            "'" + attention + "', " +
            "'" + addressLine1 + "', " +
            "'" + addressLine2 + "', " +
            "'" + zipID + "', " +
            "'" + accountNumber + "');";

        // creates and executes the query to insert the new address
        H2Access.createAndExecute(this.connection, query);
    }

    /**
     * Gets all addresses that a customer has sent packages to.
     *
     * @param accountNumber  The account number of the user.
     *
     * @return ResultSet of all addresses sent to by this user.
     */
    public ResultSet getAllAddressesSentTo(String accountNumber) {
        String query = "SELECT * FROM address " + "WHERE (ACCOUNT_NUMBER_FK = " + accountNumber + ");";
        // run the query
        return H2Access.createAndExecuteQuery(this.connection, query);
    }

    /**
     * Sets the customer's 'home' address from the address they've used.
     *
     * Return code
     * 0 - Success
     * 1 - The addressId provided does not exist or does not belong to this user
     * 2 - The home address could not be updated to key of the new address
     * 3 - Unknown error
     *
     * @param addressId The address ID to set as the "home" address.
     *
     * @return An integer status code.
     */
    public int setHomeAddress(int addressId) {
        // gets the account number of this user
        int accountNumber = H2Access.getUserFK(this.username);

        // a query to determine if a user already has this address as their home
        String existsQuery = "SELECT account_number_fk FROM address WHERE id=" + addressId;

        // used to update a customer's mailing address ID
        String setQuery = "UPDATE customer SET mailing_address_id_fk=%d WHERE account_number=%d";

        try {
            // executes the first select query to see if someone already uses this addressID as their home address
            ResultSet queryResult = H2Access.createAndExecuteQuery(
                this.connection,
                existsQuery
            );

            // if the queryResult has a next value
            if (queryResult.next()) {
                // if the current user with this as their home address,
                // return success (it's already been done)
                if (queryResult.getInt(1) == accountNumber) {
                    return 0;
                }
                // return 1 as this home address cannot be shared by multiple users
                else {
                    return 1;
                }
            }

            // formats the set query
            String formattedSetQuery = String.format(
                setQuery,
                addressId,
                accountNumber
            );

            // returns if the createAndExecute method succeeded
            if (H2Access.createAndExecute(this.connection, formattedSetQuery)) {
                return 0;
            }
        } catch (SQLException e) {
            // and SQLException will return 3
            e.printStackTrace();
            return 3;
        }

        // return the fact that it could not be set
        return 2;
    }

    /**
     * A method that looks up a zip code based off of a city and a state.
     *
     * @param city  The city to look up.
     * @param state  The state to look up.
     *
     * @return ResultSet of zips for that city/state combo
     */
    public ResultSet zipCodeLookupByCityState(String city, String state) {
        String query =
                "SELECT * FROM ZIP_CODE WHERE (CITY = '" + city + "') AND (STATE ='" + state + "');";
        return H2Access.createAndExecuteQuery(this.connection, query);
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

    /**
     *
     * @param accountNumber  The account number of the user sending the package.
     * @param serviceID  The service ID of the service being used.
     * @param height  The height of the package.
     * @param length  The length of the package.
     * @param depth  The length of the package.
     * @param weight  The weight of the package.
     * @param originCompany  The origin company.
     * @param originAttention  The origin attention line.
     * @param originAddressLine1  The origin address line 1.
     * @param originAddressLine2  The origin address line 2.
     * @param originCity  The origin city.
     * @param originState  The origin state.
     * @param originZip  The origin zip code.
     * @param destinationCompany  The destination company.
     * @param destinationAttention  The destination attention.
     * @param destinationAddressLine1  The destination address line 1.
     * @param destinationAddressLine2  The destination address line 2.
     * @param destinationCity  The destination city.
     * @param destinationState  The destination state.
     * @param destinationZip  The destination zip code.
     *
     * @return  A result set containing information about the package; or null if it could not be created.
     * @throws SQLException  If any SQLException is encountered, it is thrown to the caller.
     */
    public ResultSet sendPackage(String accountNumber,
                                 String serviceID,
                                 String height,
                                 String length,
                                 String depth,
                                 String weight,
                                 String originCompany,
                                 String originAttention,
                                 String originAddressLine1,
                                 String originAddressLine2,
                                 String originCity,
                                 String originState,
                                 String originZip,
                                 String destinationCompany,
                                 String destinationAttention,
                                 String destinationAddressLine1,
                                 String destinationAddressLine2,
                                 String destinationCity,
                                 String destinationState,
                                 String destinationZip) throws SQLException {
        // looks up the origin zip
        ResultSet originZipLookup = zipCodeLookupByCityState(originCity, originState);
        // looks up the destination zip
        ResultSet destinationZipLookup = zipCodeLookupByCityState(destinationCity, destinationState);

        // these will be populated with whether the origin address and destination address exist already
        String originZipID;
        String destinationZipID;

        // if there is a next result in the origin lookup
        if (originZipLookup.next()) {
            // get the ID of the zip
            originZipID = originZipLookup.getString("ID");
        // if there isn't an origin zip based off of city and state, look up via zip
        } else {
            // selects the ID of the zip based off of zip code
            originZipLookup = H2Access.createAndExecuteQuery(
                this.connection,
                "SELECT ID FROM ZIP_CODE WHERE ZIP_CODE = " + originZip
            );

            // goes to the next result value
            originZipLookup.next();
            // gets the zip ID
            originZipID = originZipLookup.getString("ID");
        }

        // does the same thing as above for the destination
        if (destinationZipLookup.next()) {
            destinationZipID = destinationZipLookup.getString("ID");
        } else {
            destinationZipLookup = H2Access.createAndExecuteQuery(
                this.connection,
                "SELECT ID FROM ZIP_CODE WHERE ZIP_CODE = " + destinationZip);
            destinationZipLookup.next();
            destinationZipID = destinationZipLookup.getString("ID");
        }


        // create new addresses for the origin and destination
        createNewAddress(
            originCompany,
            originAttention,
            originAddressLine1,
            originAddressLine2,
            originZipID,
            accountNumber
        );
        createNewAddress(
            destinationCompany,
            destinationAttention,
            destinationAddressLine1,
            destinationAddressLine2,
            destinationZipID,
            accountNumber
        );

        // get the IDs of the addresses just inserted into the table
        String originAddressID = findLocation(originZipID, 'O');
        String destinationAddressID = findLocation(destinationZipID, 'D');

        // creates the package and takes its return as packageCreation
        ResultSet packageCreation = createPackage(
            accountNumber,
            serviceID,
            height,
            length,
            depth,
            weight,
            originAddressID,
            destinationAddressID
        );


        // the next phase is to charge the customer
        ResultSet rates = getCustomerRates(accountNumber);
        int billableWeight;
        // parses out the height, length, depth, and weight to ints
        int _height = Integer.parseInt(height);
        int _length = Integer.parseInt(length);
        int _depth = Integer.parseInt(depth);
        int _weight = Integer.parseInt(weight);

        // if getting the rates has a returned row AND package creation has a returned row
        if (rates.next() && packageCreation.next()) {
            // gets the dim_rating_break
            int dimensionalDivisor = rates.getInt("dim_rating_break");
            // calculates the billable weight
            billableWeight = _height * _length * _depth;
            // divides it by the dimensional divisor
            billableWeight /= dimensionalDivisor;

            // if the actual weight is larger than "billableWeight" based off of package dimensions
            if (_weight > billableWeight) {
                billableWeight = _weight;
            }

            // creates the charge for the customer
            createCharges(
                rates,
                accountNumber,
                packageCreation.getString("serial"),
                billableWeight,
                serviceID
            );

            // updates the billing amount due
            updateBillingAmountDue(accountNumber);

            // returns the package creation
            return packageCreation;
        // returns null if the package could not be created
        } else {
            return null;
        }
    }

    /**
     * updates billing amount due
     *
     * @param acct
     * @return
     * @throws SQLException
     */
    private ResultSet updateBillingAmountDue(String acct) throws SQLException {
        // get current outstanding charges (from charge)
        // & get total paid (from charge)
        String Qoutstanding = "SELECT SUM(price) FROM CHARGE WHERE account_number_fk = '" + acct + "' AND paid = 0;";
        ResultSet outstanding = H2Access.createAndExecuteQuery(connection, Qoutstanding);
        if (outstanding.next()) {
            double due = outstanding.getDouble(1);
            String QcurrentBal = "SELECT balance_to_date FROM billing WHERE account_number_fk = '" + acct + "';";
            ResultSet currentBalance = H2Access.createAndExecuteQuery(connection, QcurrentBal);
            if (currentBalance.next()) {
                due += currentBalance.getDouble(1);
                // update billing table
                String QupdateOutstanding = "UPDATE billing SET balance_to_date = " + due + " WHERE account_number_fk = '" + acct + "';";
                if (H2Access.createAndExecute(connection, QupdateOutstanding)) {
                    String QpullUpdate = "SELECT * FROM billing WHERE account_number_fk = '" + acct + "';";
                    return H2Access.createAndExecuteQuery(connection, QpullUpdate);
                } else {
                    return null;
                }
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Calculates charges based on rates, billable weight, service.
     * Adds record in charge table
     * returns empty ResultSet
     *
     * @param rate
     * @param account_number_fk
     * @param serial
     * @param billable_weight
     * @param service_id
     * @return
     * @throws SQLException
     */
    private ResultSet createCharges(ResultSet rate, String account_number_fk, String serial,
                                    int billable_weight, String service_id) throws SQLException {
        int service = Integer.parseInt(service_id);
        double svc_multiplier = 1.0;
        int priority = service / 4;
        int hazardous = (service / 2) % 2;
        int signature = (service % 2);
        double base;
        double rush;
        if (signature == 0) {
            svc_multiplier += .05;
        }
        if (hazardous == 1) {
            svc_multiplier += .15;
        }
        if (priority > 2) {
            base = rate.getDouble(2);
        } else {
            base = rate.getDouble(1);
        }
        if (1 == priority % 2) {
            rush = rate.getDouble(3);
        } else {
            rush = 1.0;
        }
        double totalprice = ((double) billable_weight) * base * rush * svc_multiplier;
        String QInsert = "INSERT INTO CHARGE (PRICE, ACCOUNT_NUMBER_FK, PACKAGE_SERIAL_FK, SERVICE_ID, PAID) " +
                "VALUES(" +
                totalprice + ", " +
                account_number_fk + ", '" +
                serial + "', " +
                service_id + ", " +
                "0 );";
        if (H2Access.createAndExecute(connection, QInsert)) {
            String Qecho = "SELECT * FROM CHARGE WHERE ACCOUNT_NUMBER_FK = '" + account_number_fk +
                    "' and PACKAGE_SERIAL_FK = '" + serial + "';";
            return H2Access.createAndExecuteQuery(connection, Qecho);
        } else {
            return null;
        }

    }

    /**
     * gets customer's current negotiated rates.
     *
     * @param account_number
     * @return
     * @throws SQLException
     */
    public ResultSet getCustomerRates(String account_number) throws SQLException {
        String QrateID = "SELECT negotiated_rate_ID_fk FROM CUSTOMER WHERE account_number = '" + account_number + "';";
        ResultSet rateID = H2Access.createAndExecuteQuery(connection, QrateID);
        if (rateID.next()) {
            int rate_fk = rateID.getInt("negotiated_rate_ID_fk");
            String QGetRates = "SELECT * FROM RATE WHERE negotiated_rate_ID = " + rate_fk + ";";
            ResultSet rates = H2Access.createAndExecuteQuery(connection, QGetRates);
            return rates;
        } else {
            return null;
        }


    }

    /**
     * helper method for sendPackage
     * Creates package serial.
     *
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
                                    String dim_depth, String weight, String origin, String destination) throws SQLException {

        String getLast = "SELECT MAX(serial) FROM package WHERE account_number_fk = '" + account_number_fk + "';";

        ResultSet last = H2Access.createAndExecuteQuery(connection, getLast);
        // GET the last serial this customer has sent, add 1
        String lastSerial;
        if (last.next()) {

            lastSerial = last.getString(1);

        } else {
            lastSerial = "AAAAAA";
        }
        char nextSerial[] = new char[6];
        boolean carry = false;


        int i = 5;
        if (lastSerial.charAt(i) == 'Z') {
            nextSerial[i] = '0';

        } else if (lastSerial.charAt(i) == '9') {
            nextSerial[i] = 'A';
            carry = true;

        } else {
            nextSerial[i] = (char) (lastSerial.charAt(i) + 1);
        }
        if (!carry) {
            for (int j = 4; j < 0; j--) {
                nextSerial[j] = (char) (lastSerial.charAt(i) + 1);
            }
        }
        while (carry) {
            // this will work fine until someone mails their
            // 2176782336th package ( little over 2billion )
            // so i guess we're not aiming to work with amazon

            i--;
            if (lastSerial.charAt(i) == 'Z') {
                nextSerial[i] = '0';
                carry = false;

            } else if (lastSerial.charAt(i) == '9') {
                nextSerial[i] = 'A';
                carry = true;

            } else {
                nextSerial[i] = (char) (lastSerial.charAt(i) + 1);
                carry = false;
            }
        }
        while (i >= 0) {
            nextSerial[i] = lastSerial.charAt(i);
            i--;
            // this should now copy over everything that isn't carry-digited
        }


        String query = "INSERT INTO PACKAGE " +
                "(ACCOUNT_NUMBER_FK, SERVICE_ID_FK, SERIAL, HEIGHT, LENGTH, DEPTH, WEIGHT, ORIGIN_FK, DESTINATION_FK)" +
                " VALUES (" +
                account_number_fk + ", " +
                service_id_fk + ", " +
                "'" + new String(nextSerial) + "', " +
                dim_height + ", " +
                dim_length + ", " +
                dim_depth + ", " +
                weight + ", " +
                "'" + origin + "', " +
                "'" + destination + "');";
        if (H2Access.createAndExecute(connection, query)) {

            String query2 = "SELECT * FROM PACKAGE" +
                    " WHERE (ACCOUNT_NUMBER_FK = " + account_number_fk +
                    ") AND (SERIAL = '" + new String(nextSerial) + "');";

            ResultSet ins_new_rec = H2Access.createAndExecuteQuery(connection, query2);
            return ins_new_rec;
        } else {
            return null;
        }
    }

    /**
     * helper method for finding location strings based on address ID + whether is destination
     * or origin
     *
     * @param address_id_fk
     * @param constraint
     * @return location string
     * @throws SQLException
     */
    // Use capital letters; not set up to handle lower case
    private String findLocation(String address_id_fk, char constraint) throws SQLException {
        String query = "SELECT * FROM LOCATION WHERE ADDRESS_ID = " + address_id_fk + ";";
        ResultSet matches = H2Access.createAndExecuteQuery(connection, query);
        // new changes
        if (((constraint == 'O') || (constraint == 'D') || (constraint == 'o') || (constraint == 'd')) && (matches.next())) {
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
        String query2;
        ResultSet ok;
        char[] ch_to_array = new char[12];
        if ((constraint == 'O') || (constraint == 'D')) {
            ch_to_array[0] = 'T';
            ch_to_array[1] = constraint;
        } else {
            ch_to_array[0] = constraint;
            ch_to_array[1] = (char) (r.nextInt(10) + '0');
        }
        do {
            ch_to_array[2] = (char) (r.nextInt(26) + 'A');
            ch_to_array[3] = (char) (r.nextInt(26) + 'A');
            ch_to_array[4] = (char) (r.nextInt(26) + 'A');
            ch_to_array[5] = (char) (r.nextInt(26) + 'A');
            ch_to_array[6] = (char) (r.nextInt(26) + 'A');
            ch_to_array[7] = (char) (r.nextInt(26) + 'A');
            ch_to_array[8] = (char) (r.nextInt(26) + 'A');
            ch_to_array[9] = (char) (r.nextInt(26) + 'A');
            ch_to_array[10] = (char) (r.nextInt(26) + 'A');
            ch_to_array[11] = (char) (r.nextInt(26) + 'A');
            query2 = "SELECT * FROM LOCATION WHERE ID = '" + String.copyValueOf(ch_to_array) + "';";
            ok = H2Access.createAndExecuteQuery(connection, query2);

        } while (ok.next());
        return String.copyValueOf(ch_to_array);

    }

    /**
     * A function which returns a ResultSet containing the last three transactions involved with this account.
     *
     * @param accntNum The account number to get the last three transactions on.
     * @return A ResultSet containing UP TO 3 transactions.
     */
    public ResultSet getLastThreeTransactions(int accntNum) {
        String query = "SELECT date, time, transaction.account_number_fk, " +
                "package_serial_fk, location_ID_fk, package.service_id_fk " +
                "FROM transaction " +
                "INNER JOIN package ON package_serial_fk = package.serial " +
                "WHERE transaction.account_number_fk = " + accntNum + " " +
                "ORDER BY date, time " +
                "LIMIT 3;";
        try {
            Connection conn = H2Access.createConnection("me", "password");
            return H2Access.createAndExecuteQuery(conn, query);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Function required to be implemented by AutoCloseable.
     * Lets CustomerAccess be used in a 'try with resources' block
     */
    @Override
    public void close() {
        H2Access.closeConnection(this.connection);
    }
}
