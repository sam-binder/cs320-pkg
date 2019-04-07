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
        this.connection = this.h2.createConnection(username, password);
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
                            String streetLineTwo, int zipCode){
        String zipQuery = "SELECT id FROM zip_code WHERE zip_code = " + zipCode + ";";
        String addressQuery = "INSERT INTO " +
                "address(company, attn, street_line_1, street_line_2, zip_fk, account_number_fk) " +
                "VALUES('%s', '%s', '%s', '%s', %d, %d);";
        String customerQuery = "UPDATE customer SET mailing_address_id_fk=%d WHERE account_number=%d";
        ResultSet r;
        int zipFK;
        int addressID = 0;

        // Get the user's fk
        int userFK = h2.getUserFK(username);
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
     * Call this one when adding an address
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
            does_it_exist = createAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
            does_it_exist = retrieveAddress(company, attention, streetLine1, streetLine2, zip_ID_fk, account_number_fk);
        }

        return does_it_exist;

    }

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
                "(ZIP_ID_FK = "+ zip_ID_fk_numeric + ") AND " +
                "(ACCOUNT_NUMBER_FK = " + account_number_fk_numeric + ");";
        return h2.createAndExecuteQuery(connection, query);
    }
    /**
     * create a new address for a customer. Called after querying whether this address already exists.
     * @param company
     * @param attention
     * @param streetLine1
     * @param streetLine2
     * @param zip_ID_fk
     * @param account_number_fk
     * @return the result of the query, which should be empty.
     */
    private ResultSet createAddress(String company, String attention, String streetLine1, String streetLine2,
                              String zip_ID_fk, String account_number_fk){
        int zip_ID_fk_numeric = Integer.parseInt(zip_ID_fk);
        int account_number_fk_numeric = Integer.parseInt(account_number_fk);
        String query =
                "INSERT INTO address "+
                "(COMPANY, ATTN, STREET_LINE_1, STREET_LINE_2, ZIP_ID_FK, ACCOUNT_NUMBER_FK)" +
                " VALUES(" +
                company + ", " +
                attention + ", " +
                streetLine1 + ", " +
                streetLine2 + ", " +
                zip_ID_fk_numeric + ", " +
                account_number_fk_numeric + ");";



        return h2.createAndExecuteQuery(connection, query);
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
     *
     * @param city
     * @param state
     * @return
     */
    public ResultSet zipCodeLookupByCityState(String city, String state){
        String query =
                "SELECT * FROM ZIP WHERE (CITY = '" + city + "') AND (STATE ='" + state + "');";
        return h2.createAndExecuteQuery(connection, query);
    }

    /*
        FULLY UI FACING - invoke from server
        Before inserting:
            -looks up zip codes for destination, origin
                if not found, uses first appropriate city/state match (or throw exception if no city/state match)
            -looks up OR creates addresses for destination, origin
        returns createPackage after those have been done
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
        do {
            if (o_zip.getInt(0) == o_zip_numeric ){
                origin_has_addr = o_zip.getString(1);
            }
        } while (o_zip.next());
        if(origin_has_addr.equals("none")){
            o_zip.first();
            origin_has_addr = o_zip.getString(1);
        }
        do{
            if (d_zip.getInt(0) == d_zip_numeric ){
                dest_has_addr = d_zip.getString(1);
            }
        } while (d_zip.next());
        if(dest_has_addr.equals("none")){
            d_zip.first();
            dest_has_addr = d_zip.getString(1);
        }

        // get address fks
        ResultSet origin_RS = createNewAddress(origin_company, origin_attention, origin_street1, origin_street2,
                origin_has_addr, account_number_fk);
        ResultSet destination_RS = createNewAddress(destination_company, destination_attention, destination_street1,
                destination_street2, dest_has_addr, account_number_fk);

        // get location fks
        String origin_id = find_location(origin_has_addr, 'O');
        String destination_id = find_location(dest_has_addr, 'D');

        return createPackage(account_number_fk, service_id_fk, dim_height, dim_length, dim_depth, weight, origin_id, destination_id);



    }

    private ResultSet createPackage(String account_number_fk, String service_id_fk, String dim_height, String dim_length,
                                    String dim_depth, String weight, String origin, String destination){
        //TODO


    }

    // Use capital letters; not set up to handle lower case
    private String find_location(String address_id_fk, char constraint) throws SQLException {
        String query = "SELECT * FROM LOCATION WHERE ADDRESS_ID_FK = " + address_id_fk + ";";
        ResultSet matches = h2.createAndExecuteQuery(connection, query);
        if( (constraint == 'O')||(constraint == 'D')||(constraint == 'o')||(constraint == 'd')) {
            do {
                switch (constraint) {
                    case 'O':
                    case 'o':
                        if (matches.getString(0).charAt(1) == 'O') {
                            return matches.getString(0);
                        }
                        break;
                    case 'D':
                    case 'd':
                        if (matches.getString(0).charAt(1) == 'D') {
                            return matches.getString(0);
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

    //Stubbed out method for future functionality
    public void addMoneyToAccount(){

    }

    /**
     * Ease of use function to view data about a package.
     * @param accntNum int: The account number of who order the package
     * @param serial string: The package's serial number
     * @return A ResultSet of data about the package, to be processed by the client
     */
    public ResultSet viewPackageData(int accntNum, String serial){
        String query = "SELECT * from package WHERE account_number_fk=" + accntNum +
                " AND serial='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }

    /**
     * Ease of use function to view the history of a package.
     * @param accntNum int: The account number of who order the package
     * @param serial string: The package's serial number
     * @return A ResultSet of data about the package's history, to be processed by the client
     */
    public ResultSet trackPackage(int accntNum, String serial){
        String query = "SELECT * from transaction WHERE account_number_fk=" + accntNum +
                " AND package_serial_fk='" + serial +"'";
        return h2.createAndExecuteQuery(connection, query);
    }



    /**
     * Function required to be implemented by AutoCloseable.
     * Lets CustomerAccess be used in a 'try with resources' block
     */
    @Override
    public void close(){
        h2.closeConnection(this.connection);
    }
}
