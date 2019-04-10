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
	private static String dbLocation = "./src";

	// Database getter
	public static String getDbLocation() {
		return dbLocation;
	}

	// Database seter
	public static void setDbLocation(String dbLocation) {
		dbLocation = dbLocation;
	}

	/**
	 * Creates and returns a database connection with the given params. After the
	 * connection has been used, should be closed with closeConnection()
	 *
	 * @param user:     user name for the user logging in
	 * @param password: password of the user logging in
	 * @throws SQLException if the connection fails
	 */
	public static Connection createConnection(String user, String password) throws SQLException {
		try {
			//This needs to be on the front of your location
			String url = "jdbc:h2:" + dbLocation;
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
	 *
	 * @param conn a Connection object to close.
	 * @return boolean False if the connection couldn't be closed, true otherwise.
	 */
	public static boolean closeConnection(Connection conn) {
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
	 *
	 * @param conn  Connection: a valid connection to the database
	 * @param query String: a query
	 * @return ResultSet: a ResultSet of data returned
	 */
	public static ResultSet createAndExecuteQuery(Connection conn, String query) {
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
	 *
	 * @param conn  Connection: a valid connection to the database
	 * @param query String: a query
	 * @return Boolean: If the query executed without errors
	 */
	public static boolean createAndExecute(Connection conn, String query) {
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
	 *
	 * @param username String: a user's username
	 * @return String: The name of the table that the user is in (customer, accounting_employee, package_employee).
	 */
	public static String getUserType(String username) {
		String s = null;
		try {
			Connection conn = createConnection("me", "password");
			String query = "SELECT type FROM user where username='" + username + "'";
			ResultSet r = createAndExecuteQuery(conn, query);
			if (r.next())
				s = r.getString(1);
			closeConnection(conn);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return s;
	}

	/**
	 * Gets the foreign key of a user
	 *
	 * @param username
	 * @return
	 */
	public static int getUserFK(String username) {
		try {
			Connection conn = createConnection("me", "password");
			String query = "SELECT general_fk FROM user " +
					" WHERE username = '" + username + "';";
			ResultSet r = conn.createStatement().executeQuery(query);
			if (r.next())
				return r.getInt(1);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Attempts to create a customer. Creates an entry in the user table with the username
	 * and password, and an entry in the customer table with mostly null values
	 * Error codes:
	 * 0	-	Successfully created a user.
	 * 1	-	Another user already has the username.
	 * 2	-	Database already in use.
	 * 3	- 	User didn't exists in the "USER" table, but did in the "Users" table.
	 * 4   -   Unable to get the customers ID
	 * -1	-	An unknown error occurred.
	 *
	 * @param username The new customer's username
	 * @param password The new customer's password
	 * @return int: an int error or success number, see doc.
	 */
	public static int createCustomer(String username, String password) {
		int errorCode = 0;
		int accountID;
		// Create a blank entry in the customer table, to be filled in by the user later
		// Starts off with the default rate
		String customerQuery = "INSERT INTO customer(BILLING_FK, NEGOTIATED_RATE_ID_FK, " +
				"MAILING_ADDRESS_ID_FK, FIRST_NAME, LAST_NAME, PHONE_NO) " +
				"VALUES(null, 1, null, null, null, null)";
		String userQuery = String.format("INSERT INTO user VALUES(0, '%s','%s','%s');",
				username, password, "customer");
		String updateUserQuery = "UPDATE user SET general_fk=%d WHERE username='" + username + "';";

		try {
			Connection conn = createConnection("me", "password");
			conn.createStatement().execute(userQuery);
			conn.createStatement().execute("CREATE USER " + username + " PASSWORD '" + password + "';");
			closeConnection(conn);
		} catch (JdbcSQLIntegrityConstraintViolationException e) {
			errorCode = 1;
		} catch (JdbcSQLNonTransientConnectionException e) {
			errorCode = 2;
		} catch (JdbcSQLSyntaxErrorException e) {
			errorCode = 3;
		} catch (SQLException e) {
			e.printStackTrace();
			errorCode = -1;
		}

		// If we pass the user validation, create an empty customer in the customer table,
		// with values to be filled in later, and edit the FK on our new user.
		if (errorCode == 0) {
			try {
				Connection conn = createConnection("me", "password");
				PreparedStatement prep = conn.prepareStatement(customerQuery, Statement.RETURN_GENERATED_KEYS);
				prep.executeUpdate();
				ResultSet keys = prep.getGeneratedKeys();
				if (keys.next())
					accountID = keys.getInt("ACCOUNT_NUMBER");
				else
					return 4;
				conn.createStatement().execute(String.format(updateUserQuery, accountID));
				closeConnection(conn);
			} catch (SQLException e) {
				e.printStackTrace();
				errorCode = -1;
			}
		}
		return errorCode;
	}

	/**
	 * Ease of use function to view data about a package.
	 *
	 * @param accntNum int: The account number of who order the package
	 * @param serial   string: The package's serial number
	 * @return A ResultSet of data about the package, to be processed by the client
	 */
	public static ResultSet viewPackageData(int accntNum, String serial) {
		String query = "SELECT * from package WHERE account_number_fk=" + accntNum +
				" AND serial='" + serial + "'";
		try {
			Connection conn = createConnection("me", "password");
			return createAndExecuteQuery(conn, query);
		} catch (SQLException e) {
			return null;
		}
	}

	/**
	 * Ease of use function to view the history of a package.
	 *
	 * @param accntNum int: The account number of who order the package
	 * @param serial   string: The package's serial number
	 * @return A ResultSet of data about the package's history, to be processed by the client
	 */
	public static ResultSet trackPackage(int accntNum, String serial) {
		String query = "SELECT transaction.*, zip_code.city, zip_code.state from transaction " +
				"INNER JOIN location ON location_id_fk = location.ID " +
				"INNER JOIN address ON address.ID = location.address_id " +
				"INNER JOIN zip_code ON zip_code.ID = address.zip_fk " +
				"WHERE transaction.account_number_fk=" + accntNum +
				" AND package_serial_fk='" + serial + "' ORDER BY date, time;";
		try {
			Connection conn = createConnection("me", "password");
			return createAndExecuteQuery(conn, query);
		} catch (SQLException e) {
			return null;
		}
	}

	public static ResultSet getLastThreeTransactions(int accntNum) {
		String query = "SELECT date, time, transaction.account_number_fk, " +
				"package_serial_fk, location_ID_fk, package.service_id_fk " +
				"FROM transaction " +
				"INNER JOIN package ON package_serial_fk = package.serial " +
				"WHERE transaction.account_number_fk = " + accntNum + " " +
				"ORDER BY date, time " +
				"LIMIT 3;";
		try {
			Connection conn = createConnection("me", "password");
			return createAndExecuteQuery(conn, query);
		} catch (SQLException e) {
			return null;
		}
	}

	public static void clearDatabase() {
		// remove all users but me, then drop all tables
		try {
			Connection conn = H2Access.createConnection("me", "password");
			ResultSet r = H2Access.createAndExecuteQuery(conn, "Select username from user;");
			while (r.next()) {
				String user = r.getString(1);
				if (!user.equalsIgnoreCase("me"))
					H2Access.createAndExecute(conn, "drop user if exists " + user);
			}
			String dropTable = "DROP TABLE accounting_employee, address, billing, " +
					"charge, customer, location, package, package_employee, priority, " +
					"rate, service, transaction, user, zip_code;";
			H2Access.createAndExecute(conn, dropTable);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Starts and runs the database
	 *
	 * @param args: not used but you can use them
	 */
	public static void main(String[] args) {
		//H2Access h2 = new H2Access();

		//NOTE: Set to true if AND ONLY IF this is the first time running the code.
		boolean firstTime = false;
		if (firstTime)
			new CreateNewDatabase().initDatabase();
		//H2Access.clearDatabase();

		// Example creating a Customer
		System.out.println("Creating the customer returned: " + createCustomer("Walter", "password"));
		try (CustomerAccess c = new CustomerAccess("Walter", "password")) {
			System.out.println(c.changeBasicInformation("Walter", "Schartl", "585-867-5309"));
			System.out.println(c.changeBasicInformation(null, "Schaertl", null));
			System.out.println(c.setUpBillingInfo());
			System.out.println(c.enterAddress("this co", "Walter S", "1123 lone rd", "", 14548));
			System.out.println(c.enterAddress("this co", "Walter s", "Bahamas", "", 14540));
			ResultSet r = c.getAllAddresses(H2Access.getUserFK("Walter") + "");
			if (r.next()) {
				int id = r.getInt(1);
				c.setHomeAddress(id);
			}

			System.out.println(c.setHomeAddress(200));
		} catch (SQLException e) {
			e.printStackTrace();
		}


		// Example usage of ease functions
		System.out.println("\nSample checking a user's type.");
		System.out.println(getUserType("AAAA"));

		System.out.println("\nSample Package Employee updating a packages location.");
		try (EmployeeAccess employee1 = new EmployeeAccess("CCCC", "password", getUserType("CCCC"))) {
			System.out.println("User ID: " + employee1.getId());
			employee1.updatePackageLocation("TOT0MYPN2PLK", 131, "B9IWEA");
		} catch (SQLException sqle) {
			sqle.printStackTrace();
		}

		try (EmployeeAccess employee2 = new EmployeeAccess("BBBB", "password", getUserType("BBBB"))) {
			System.out.println("\nSample Accounting Employee looking up customers named Amy.");
			ResultSet results = employee2.getCustomersWhere("first_name='Amy'");
			while (results.next()) {
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
			while (results.next()) {
				System.out.printf("Billing %d: %f %s %d %d\n",
						results.getInt(1),
						results.getDouble(2),
						results.getString(3),
						results.getInt(4),
						results.getInt(5));
			}
			System.out.println("\nSample Accounting Employee tracking a packages location.");
			results = employee2.viewPackageHistory(131, "B9IWEA");
			while (results.next()) {
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
			while (results.next()) {
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
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}
}
