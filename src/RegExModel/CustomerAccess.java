package RegExModel;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by Walter Schaertl on 3/24/2019.
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

    //Stubbed out method for future functionality
    public void createAddress(){

    }

    //Stubbed out method for future functionality
    public void sendPackage(){

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
