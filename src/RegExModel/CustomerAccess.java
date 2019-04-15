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
        return H2Access.createAndExecuteQuery(this.connection, query);
        // run the query
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
        ResultSet newOrig = createNewAddress(
            originCompany,
            originAttention,
            originAddressLine1,
            originAddressLine2,
            originZipID,
            accountNumber
        );
        ResultSet newDest = createNewAddress(
            destinationCompany,
            destinationAttention,
            destinationAddressLine1,
            destinationAddressLine2,
            destinationZipID,
            accountNumber
        );

        newOrig.next();
        newDest.next();


        // get the IDs of the addresses just inserted into the table
        String originAddressID = findLocation(newOrig.getString(1), 'O');
        String destinationAddressID = findLocation(newDest.getString(1), 'D');

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
     * Updates the amount that is due by a customer.
     *
     * @param accountNumber  The account number of the user.
     *
     * @throws SQLException  If any SQLException is encountered, it is thrown out to the caller
     */
    private void updateBillingAmountDue(String accountNumber) throws SQLException {
        // a query to select the current outstanding amount due by this customer
        String currentUnpaidCharges =
            "SELECT SUM(price) " +
            "FROM CHARGE " +
            "WHERE account_number_fk = '" + accountNumber + "' AND paid = 0;";

        // queries the outstanding amount due
        ResultSet queryUnpaidCharges = H2Access.createAndExecuteQuery(
            this.connection,
            currentUnpaidCharges
        );

        // if there is an outstanding amount returned above
        if (queryUnpaidCharges.next()) {
            // pulls out the amount due by this customer
            double amountDue = queryUnpaidCharges.getDouble(1);

            // a query used to get the balance to date
            String currentBalanceToDate =
                "SELECT balance_to_date " +
                "FROM billing " +
                "WHERE account_number_fk = '" + accountNumber + "';";
            // runs the above query
            ResultSet currentBalance = H2Access.createAndExecuteQuery(this.connection, currentBalanceToDate);

            // if the above query returned a result
            if (currentBalance.next()) {
                // the amount due gets updated with the current balance outstanding
                amountDue += currentBalance.getDouble(1);

                // builds a query to update the balance to date for this customer
                String updateOutstandingBalanceDue =
                    "UPDATE billing " +
                    "SET balance_to_date = " + amountDue + " " +
                    "WHERE account_number_fk = '" + accountNumber + "';";


                // if this udpate runs to completion
                if (H2Access.createAndExecute(
                    this.connection,
                    updateOutstandingBalanceDue
                )) {
                    // builds a query to get the amount due from billing
                    String queryAmountDue =
                        "SELECT * " +
                        "FROM billing " +
                        "WHERE account_number_fk = '" + accountNumber + "';";
                    // returns the result of the query to get the amount due
                    H2Access.createAndExecuteQuery(this.connection, queryAmountDue);
                }
            }
        }
    }

    /**
     * Calculates the cost of a charge based on rate, billable weight and service type.
     *
     * @param rate  The rate result set as found by the calling method.
     * @param accountNumber  The account number of the user being charged.
     * @param serial  The serial of the package to be charging for.
     * @param billableWeight  The billable weight of the package.
     * @param serviceId  The service ID being used to ship this package.
     *
     * @return  The just-inserted charge.
     * @throws SQLException  Any SQLException is thrown out to the calling method.
     */
    private ResultSet createCharges(ResultSet rate,
                                    String accountNumber,
                                    String serial,
                                    int billableWeight,
                                    String serviceId) throws SQLException {
        // parses the service ID
        int service = Integer.parseInt(serviceId);
        // default service multiplier is 1.0
        double serviceMultiplier = 1.0;

        // the priority is the service ID INT divided by 4
        int priority = service / 4;

        // calculates the hazardous value
        int hazardous = (service / 2) % 2;

        // determines if a signature is needed
        int signature = (service % 2);

        // if no signature required
        if (signature == 0) {
            // add .05 to the service multiplier
            serviceMultiplier += .05;
        }

        // shipping hazardous material costs a bit more
        if (hazardous == 1) {
            serviceMultiplier += .15;
        }

        // gets the base based off of the priority level
        double baseMultiplier = rate.getDouble((priority > 2) ? 2 : 1);

        // gets the rush multiplier
        double rushMultiplier = (priority % 2 == 1) ? rate.getDouble(3) : 1.0;

        // calculates the total price: the billable weight multiplied by the base, the rush and service multipliers
        double totalprice = billableWeight * baseMultiplier * rushMultiplier * serviceMultiplier;
        String realprice = String.format("%.2f", totalprice);

        String insertionQuery =
            "INSERT INTO charge " +
            "(price, account_number_fk, package_serial_fk, service_id, paid) " +
            "VALUES(" +
                realprice + ", " +
                accountNumber + ", '" +
                serial + "', " +
                serviceId + ", " +
                "0 " +
            ");";

        // attempts to insert the charge into the database
        if (H2Access.createAndExecute(this.connection, insertionQuery)) {
            // if that succeeds, return the row just created
            return H2Access.createAndExecuteQuery(
                this.connection,
                "SELECT * " +
                "FROM charge " +
                "WHERE account_number_fk = '" + accountNumber + "' " +
                "AND PACKAGE_SERIAL_FK = '" + serial + "';"
            );
        }

        // any fall through will return null
        return null;
    }

    /**
     * Gets negotiated rates for a customer.
     *
     * @param accountNumber  The account number for a customer.
     *
     * @return  The negotiated rate row from the database for that user.
     * @throws SQLException  If any SQLException is encountered, it is thrown to the caller.
     */
    private ResultSet getCustomerRates(String accountNumber) throws SQLException {
        // builds the query to get the rate ID for this customer with accountNumber
        String rateIDQuery =
            "SELECT negotiated_rate_ID_fk " +
            "FROM customer " +
            "WHERE account_number = '" + accountNumber + "';";

        // gets the rateID for the user
        ResultSet rateIDQueryRes = H2Access.createAndExecuteQuery(connection, rateIDQuery);

        // if there exists a row in the table
        if (rateIDQueryRes.next()) {
            // get the rate foreign key
            int rateID = rateIDQueryRes.getInt("negotiated_rate_ID_fk");

            String getRatesQuery =
                "SELECT * " +
                "FROM rate " +
                "WHERE negotiated_rate_ID = " + rateID + ";";

            // returns the query result of rate retrieval
            return H2Access.createAndExecuteQuery(connection, getRatesQuery);
        }

        // all fall through will return null
        return null;
    }

    /**
     * A helper method for sendPackage to create a package serial for the package and insert it into the database.
     *
     * @param accountNumber  The account number of the sending package.
     * @param serviceID  The service ID to send the package.
     * @param height  The height of the package.
     * @param length  The length of the package.
     * @param depth  The depth of the package.
     * @param weight  The weight of the package.
     * @param origin  The origin of the package.
     * @param destination  The destination of the package.
     *
     * @return  The ResultSet of the package created for the package.
     */
    private ResultSet createPackage(String accountNumber,
                                    String serviceID,
                                    String height,
                                    String length,
                                    String depth,
                                    String weight,
                                    String origin,
                                    String destination) throws SQLException {

        String getLastPackageSentQuery =
            "SELECT MAX(serial) " +
            "FROM package " +
            "WHERE account_number_fk = '" + accountNumber + "';";

        // queries the last package
        ResultSet lastPackage = H2Access.createAndExecuteQuery(
            this.connection,
            getLastPackageSentQuery
        );

        // get the last serial this customer sent
        String lastSerial = "AAAAAA";

        // if the serial lookup had a result (customer has sent at least one package)
        if (lastPackage != null && lastPackage.next()) {
            // grab the last serial
            String possibleLastSerial = lastPackage.getString(1);
            if(possibleLastSerial != null)
                lastSerial = possibleLastSerial;
        }

        // creates a next serial char array
        char [] nextSerial = new char[6];

        // the current index in the serial
        int i = 5;

        // if the last character in the serial is a 9, there is roll over
        // that must be accounted for
        if (lastSerial.charAt(i) == '9') {
            // place an A at that index
            nextSerial[i] = 'A';

            // keep going until rollOver stops
            while(true) {
                // go to the last character in the serial
                --i;
                // if the next previous character in the last serial is another rollover character
                if (lastSerial.charAt(i) == '9') {
                    // place an A at that index; keep propagating roll over effects
                    nextSerial[i] = 'A';
                } else {
                    // else put the correct next character
                    if(lastSerial.charAt(i) == 'Z') {
                        nextSerial[i] = '0';
                    } else {
                        nextSerial[i] = (char)(lastSerial.charAt(i) + 1);
                    }

                    // and break the loop
                    break;
                }
            }
        } else {
            nextSerial[i] = (char)(lastSerial.charAt(i) + 1);
        }

        // go down through the remaining characters copying over the same character from the last serial
        for (i=4; i >= 0; --i) {
            nextSerial[i] = lastSerial.charAt(i);
        }

        // builds the insertion query to insert our package
        String packageInsertion =
            "INSERT INTO package" +
            "(account_number_fk, service_id_fk, serial, height, " +
                "length, depth, weight, origin_fk, destination_fk) " +
            "VALUES (" +
                accountNumber + ", " +
                serviceID + ", " +
                "'" + new String(nextSerial) + "', " +
                height + ", " +
                length + ", " +
                depth + ", " +
                weight + ", " +
                "'" + origin + "', " +
                "'" + destination + "'" +
            ");";

        // if the package is successfully inserted
        if (H2Access.createAndExecute(this.connection, packageInsertion)) {
            // return the package row just generated
            return H2Access.createAndExecuteQuery(
                this.connection,
                "SELECT * " +
                "FROM package " +
                "WHERE account_number_fk = " + accountNumber + " " +
                "AND serial='" + new String(nextSerial) + "';"
            );
        // else return null
        } else {
            return null;
        }
    }

    /**
     * A helper method used for finding location string based on an address ID and whether it is a destination or origin.
     *
     * @param addressID  The ID of this address.
     * @param locationType  The location type (destination or origin)
     * @return A location string.
     * @throws SQLException  Any SQLException is thrown out to the caller.
     */
    private String findLocation(String addressID, char locationType) throws SQLException {
        // a query which selects all information from location which has an address ID of addressID
        String query = "SELECT * FROM LOCATION WHERE ADDRESS_ID = " + addressID + ";";
        ResultSet matches = H2Access.createAndExecuteQuery(this.connection, query);

        // if there was a returned result from the query
        if (matches.next()) {
            do {
                // performs a switch based on the location type
                switch (locationType) {
                    case 'O':
                        // if the string matches on the second character ("TO...")
                        if (matches.getString(1).charAt(1) == 'O') {
                            return matches.getString(1);
                        }
                        break;
                    case 'D':
                        // do the same thing as above for O, except for destinations
                        if (matches.getString(1).charAt(1) == 'D') {
                            return matches.getString(1);
                        }
                        break;
                    default:
                        // this is not good
                        break;
                }
            } while (matches.next());
        }


        // creates a new random generator
        Random rand = new Random();
        // a result set used to determine if a location ID exists
        ResultSet locationIDExistsQuery;
        StringBuilder locationIDBeginner = new StringBuilder();

        if ((locationType == 'O') || (locationType == 'D')) {
            locationIDBeginner.append('T');
            locationIDBeginner.append(locationType);
        } else {
            locationIDBeginner.append(locationType);
            locationIDBeginner.append(rand.nextInt(10) + '0');
        }

        // a StringBuilder which will be used to house the location ID
        StringBuilder locationIDBuilder;

        // keeps generating location IDs until we find a unique one
        do {
            // creates a fresh locationID builder with a start of locationIDBeginner
            locationIDBuilder = new StringBuilder(locationIDBeginner);

            // appends the last 10 characters to the string builder
            for(int i = 2; i < 12; ++i) {
                locationIDBuilder.append((char)(rand.nextInt(26) + 'A'));
            }

            // queries the database to see if the location ID has been taken
            locationIDExistsQuery = H2Access.createAndExecuteQuery(
                this.connection,
                "SELECT * " +
                "FROM LOCATION " +
                "WHERE ID = '" + locationIDBuilder.toString() + "';"
            );
        } while (locationIDExistsQuery.next());

        String Qaddloc = "INSERT INTO location (ID, address_ID) VALUES ('" +
                locationIDBuilder.toString() + "', '" +
                addressID + "');";
        if(H2Access.createAndExecute(connection, Qaddloc)){
            return locationIDBuilder.toString();
        } else {
            return null;
        }
    }

    /**
     * The AutoClosable portion of this class.  Will automatically close the
     * connection to the database.
     */
    @Override
    public void close() {
        H2Access.closeConnection(this.connection);
    }
}
