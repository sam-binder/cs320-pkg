package RegExModel;
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;
import org.h2.jdbc.JdbcSQLSyntaxErrorException;

import java.net.ConnectException;
import java.sql.*;
import java.util.Arrays;

/**
 * Contains a main method to set up a new database and
 * view example functionality.
 *
 * @author Walter Schaertl, template provided
 *
 */
public class H2Access {
    // Deafult database location
	private String dbLocation = "./src";

    // Database getter
	public String getDbLocation() {
		return dbLocation;
	}

	// Database seter
	public void setDbLocation(String dbLocation) {
		this.dbLocation = dbLocation;
	}

	/**
	 * Creates and returns a database connection with the given params. After the
	 * connection has been used, should be closed with closeConnection()
	 * @param user: user name for the user logging in
	 * @param password: password of the user logging in
	 * @throws SQLException if the connection fails
	 */
	public Connection createConnection(String user, String password) throws SQLException{
		try {
			//This needs to be on the front of your location
			String url = "jdbc:h2:" + this.dbLocation;
			//This tells it to use the h2 driver
			Class.forName("org.h2.Driver");

			//creates the connection
			return DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

    /**
     * Closes a connection
     * @param conn a Connection object to close.
	 * @return boolean False if the connection couldn't be closed, true otherwise.
     */
	public boolean closeConnection(Connection conn){
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

    /**
     * Executes a query and returns data it requested.
     * @param conn Connection: a valid connection to the database
     * @param query String: a query
     * @return ResultSet: a ResultSet of data returned
     */
	public ResultSet createAndExecuteQuery(Connection conn, String query){
		try {
			Statement stmt = conn.createStatement();
			return stmt.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

    /**
     * Executes a query that expects no data to be returned.
     * @param conn Connection: a valid connection to the database
     * @param query String: a query
	 * @return Boolean: If the query executed without errors
     */
	public boolean createAndExecute(Connection conn, String query){
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

    /**
     * When given a user's username, returns the type of user it is.
     * @param username String: a user's username
     * @return String: The name of the table that the user is in (customer, accounting_employee, package_employee).
     */
	public String getUserType(String username){
		String s = null;
		try {
			Connection conn = this.createConnection("me", "password");
			String query = "SELECT type FROM user where username='" + username + "'";
			ResultSet r = this.createAndExecuteQuery(conn, query);
			if (r.next())
				s = r.getString(1);
			closeConnection(conn);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return s;
	}

	/**
	 * Attempts to create a customer.
	 * Error codes:
	 * 	0	-	Successfully created a user.
	 * 	1	-	Another user already has the username.
	 * 	2	-	Database already in use.
	 * 	3	- 	User didn't exists in the "USER" table, but did in the "Users" table.
	 * 	-1	-	An unknown error occurred.
	 * 	@param username The new customer's username
	 * 	@param password The new customer's password
	 * 	@return int: an int error or success number, see doc.
	 */
	public int createCustomer(String username, String password) {
		String query = String.format("INSERT INTO user(USERNAME, PASSWORD, type) " +
				"VALUES('%s','%s','%s');", username, password, "customer");
		try {
			Connection conn = createConnection("me", "password");
			conn.createStatement().execute(query);
			conn.createStatement().execute("CREATE USER " + username + " PASSWORD '" + password + "';");
			closeConnection(conn);
			return 0;
		} catch (JdbcSQLIntegrityConstraintViolationException e) {
			return 1;
		} catch (JdbcSQLNonTransientConnectionException e) {
			return 2;
		} catch (JdbcSQLSyntaxErrorException e){
			return 3;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}
	}

	/**
	 * Starts and runs the database
	 * @param args: not used but you can use them
	 */
	public static void main(String[] args) {
		H2Access h2 = new H2Access();

        //NOTE: Set to true if AND ONLY IF this is the first time running the code.
		boolean firstTime = false;
		if(firstTime)
			new CreateNewDatabase().initDatabase();

		// Example creating a Customer
		System.out.println("Creating the customer returned: " + h2.createCustomer("Walter", "password"));

        // Example usage of ease functions
		System.out.println("\nSample checking a user's type.");
		System.out.println(h2.getUserType("AAAA"));

		System.out.println("\nSample Package Employee updating a packages location.");
		try (EmployeeAccess employee1 = new EmployeeAccess("CCCC", "password", h2.getUserType("CCCC"))) {
			System.out.println("User ID: " + employee1.getId());
			employee1.updatePackageLocation("TOT0MYPN2PLK", 131, "B9IWEA");
		} catch(SQLException sqle) {
			sqle.printStackTrace();
		}

		try (EmployeeAccess employee2 = new EmployeeAccess("BBBB", "password", h2.getUserType("BBBB"))) {
			System.out.println("\nSample Accounting Employee looking up customers named Amy.");
			ResultSet results = employee2.getCustomersWhere("first_name='Amy'");
			while(results.next()){
				System.out.printf("Person %d: %d %d %d %s %s\n",
						results.getInt(1),
						results.getInt(2),
						results.getInt(3),
						results.getInt(4),
						results.getString(5),
						results.getString(6));
			}
			System.out.println("\nSample Accounting Employee viewing billing " +
					"information for customer with account number 16.");
			results = employee2.viewCustomerBilling(16);
			while(results.next()) {
				System.out.printf("Billing %d: %f %s %d %d\n",
						results.getInt(1),
						results.getDouble(2),
						results.getString(3),
						results.getInt(4),
						results.getInt(5));
			}
			System.out.println("\nSample Accounting Employee tracking a packages location.");
			results = employee2.viewPackageHistory(131, "B9IWEA");
			while(results.next()) {
				System.out.printf("Package History %d: %s %s %d %s %d %s\n",
						results.getInt(1),
						results.getDate(2),
						results.getTime(3),
						results.getInt(4),
						results.getString(3),
						results.getInt(4),
						results.getString(5));
			}
			System.out.println("\nSample Accounting Employee viewing a package information.");
			results = employee2.viewPackageData(131, "B9IWEA");
			while(results.next()) {
				System.out.printf("Package Date %d: %d %s %d %d %d %d %s %s %s\n",
						results.getInt(1),
						results.getInt(2),
						results.getString(3),
						results.getInt(4),
						results.getInt(5),
						results.getInt(6),
						results.getInt(7),
						results.getString(8),
                        results.getString(9),
                        results.getString(10));
			}
		} catch (SQLException e){
			e.printStackTrace();
		}

	}


}
