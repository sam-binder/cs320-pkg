package RegExModel;

// FILE: H2Access.java

// imports H2 elements
import org.h2.jdbc.JdbcSQLIntegrityConstraintViolationException;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;
import org.h2.jdbc.JdbcSQLSyntaxErrorException;

// imports all of the sql elements needed
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.PreparedStatement;
import java.sql.DriverManager;

/**
 * Methods to interface with the package database.
 *
 * @author Walter Schaertl
 * @version 04/10/2019
 */
public class H2Access {
	/**
	 * Creates and returns a database connection with the given params. After the
	 * connection has been used, should be closed with closeConnection()
	 *
	 * @param username  Username for the access.
	 * @param password  Password to access the database.
	 * @throws SQLException Any SQLException encountered while logging in is thrown to caller.
	 */
	public static Connection createConnection(String username, String password) throws SQLException {
		try {
			// Used when connecting to the database in server mode. Requires DatabaseServer to be running
			//String url = "jdbc:h2:tcp://localhost:8095/" + "~/Desktop/RIT/Year 3/Data Mang/cs320-pkg/src";

			// builds a URL to connect to the database with
			String url = "jdbc:h2:./src";

			// uses the H2Driver method
			Class.forName("org.h2.Driver");

			// creates a connection based off of H2's implementation
			return DriverManager.getConnection(url, username, password);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}

		// returns null if any issue was encountered
		return null;
	}

	/**
	 * Static method to close a connection.
	 *
	 * @param conn  A Connection object to close.
	 * @return True if the connection was closed successfully, false otherwise.
	 */
	public static boolean closeConnection(Connection conn) {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		// return true if we get out of the try block
		return true;
	}

	/**
	 * Executes a query that needs results, and returns the result.
	 * <p>
	 * NOTE: DO NOT USE THIS METHOD IF THERE IS NO RETURN FROM THE QUERY; LIKE
	 * AN INSERT, FOR EXAMPLE.  USE THE createAndExecute METHOD FOR THAT.
	 *
	 * @param conn  Connection to the database.
	 * @param query  The query to run on the database.
	 * @return A ResultSet containing the result of the select query; null if any issue was encountered.
	 */
	public static ResultSet createAndExecuteQuery(Connection conn, String query) {
		try {
			// builds a statement and executes the query
			return conn.createStatement(
					ResultSet.TYPE_SCROLL_INSENSITIVE,
					ResultSet.CONCUR_UPDATABLE
			).executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// returns null if an exception was caught
		return null;
	}

	/**
	 * Executes a query where no return value is needed.
	 *
	 * @param conn  The connection to the database.
	 * @param query The query to run on the database.
	 * @return True if the query ran without issue; false otherwise.
	 */
	public static boolean createAndExecute(Connection conn, String query) {
		try {
			// creates and executes the query
			conn.createStatement().execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}

		// if we make it through no problem, true is returned
		return true;
	}

	/**
	 * When given a user's username, returns the type of user it is.
	 *
	 * @param username The username of the user to check.
	 * @return A string with the value: "customer", "package_employee", or "accounting_employee" or NULL the username doesn't exist.
	 */
	public static String getUserType(String username) {
		// will eventually be the user type
		String userType = null;
		try {
			// creates a connection to the database
			Connection conn = createConnection("me", "password");

			// builds the query
			String query = "SELECT type FROM user where username='" + username + "'";
			// executes the query
			ResultSet r = createAndExecuteQuery(conn, query);

			// if there is a row returned from the connection
			if (r.next()) {
				userType = r.getString(1);
			}

			// closes the connection
			closeConnection(conn);
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// returns the usertype
		return userType;
	}

	/**
	 * Gets the foreign key of the user with username.
	 *
	 * @param username The username of the user to check.
	 * @return The integer foreign key of the user, or -1 if they don't exist.
	 */
	public static int getUserFK(String username) {
		try {
			// creates a connection
			Connection conn = createConnection("me", "password");
			// builds a query to get the user's foreign key
			String query = "SELECT general_fk FROM user " +
					" WHERE username = '" + username + "';";

			// queries the database to get the user's foreign key value
			ResultSet r = conn.createStatement().executeQuery(query);

			// if there is a result, return the general_fk column
			if (r.next()) {
				return r.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// return -1 if the user doesn't exist or if any issue was caught
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
	 * 4    -   Unable to get the customers ID
	 * -1	-	An unknown error occurred.
	 *
	 * @param username The new customer's username
	 * @param password The new customer's password
	 * @return An error/success code depending on the result of the customer creation. See above for details of return value.
	 */
	public static int createCustomer(String username, String password) {
		// will eventually get the user's account ID
		int accountID;

		// creates a blank entry in the customer table which will be filled in later by another
		// update to the database
		String customerQuery = "INSERT INTO customer(BILLING_FK, NEGOTIATED_RATE_ID_FK, " +
				"MAILING_ADDRESS_ID_FK, FIRST_NAME, LAST_NAME, PHONE_NO) " +
				"VALUES(null, 1, null, null, null, null)";

		// builds the query to create the new user
		String userQuery = String.format("INSERT INTO user VALUES(0, '%s','%s','%s');",
				username, password, "customer");

		// builds the query used to update the user once they exist in the databse
		String updateUserQuery = "UPDATE user SET general_fk=%d WHERE username='" + username + "';";

		try {
			// creates a connection to the database
			Connection conn = createConnection("me", "password");

			// executes the userQuery
			conn.createStatement().execute(userQuery);
			// creates a USER in the database
			conn.createStatement().execute("CREATE USER " + username + " PASSWORD '" + password + "';");

			// builds a prepared statement to insert the customer
			PreparedStatement prep = conn.prepareStatement(
					customerQuery,
					Statement.RETURN_GENERATED_KEYS
			);
			// executes the query
			prep.executeUpdate();
			// gets the keys from the query
			ResultSet keys = prep.getGeneratedKeys();
			// if there was a returned from the prepared statement
			if (keys.next()) {
				accountID = keys.getInt("ACCOUNT_NUMBER");
			} else {
				return 4;
			}

			// lastly we update the user with the new information about their customer ID
			conn.createStatement().execute(
					String.format(updateUserQuery, accountID)
			);

			// closes the connection to the database
			closeConnection(conn);
			// different errors that can be caught will cause different error codes
		} catch (JdbcSQLIntegrityConstraintViolationException e) {
			return 1;
		} catch (JdbcSQLNonTransientConnectionException e) {
			return 2;
		} catch (JdbcSQLSyntaxErrorException e) {
			return 3;
		} catch (SQLException e) {
			e.printStackTrace();
			return -1;
		}

		// returns success
		return 0;
	}

	/**
	 * Ease of use method to view package data.
	 *
	 * @param accountNum The account number of the package to view.
	 * @param serial     The serial of the package to view.
	 * @return A ResultSet of data about the package to be processed by caller; null if any issue is encountered.
	 */
	public static ResultSet viewPackageData(int accountNum, String serial) {
		try {
			// builds a query to execute about the package
			String query = "SELECT * from package WHERE account_number_fk=" + accountNum +
					" AND serial='" + serial + "'";
			// creates a connection to the database
			Connection conn = createConnection("me", "password");
			// returns the result of the query
			return createAndExecuteQuery(conn, query);
		} catch (SQLException e) {
			return null;
		}
	}

	/**
	 * Ease of use method to view history of a package.
	 *
	 * @param accountNum The account number of the package to view.
	 * @param serial     The serial of the package to view.
	 * @return A ResultSet of data about the package history to be processed by caller; null if any issue is encountered.
	 */
	public static ResultSet trackPackage(int accountNum, String serial) {
		try {
			String query = "SELECT transaction.*, zip_code.city, zip_code.state from transaction " +
					"LEFT JOIN location ON location_id_fk = location.ID " +
					"LEFT JOIN address ON address.ID = location.address_id " +
					"LEFT JOIN zip_code ON zip_code.ID = address.zip_fk " +
					"WHERE transaction.account_number_fk=" + accountNum +
					" AND package_serial_fk='" + serial + "' ORDER BY date, time;";
			Connection conn = createConnection("me", "password");
			return createAndExecuteQuery(conn, query);
		} catch (SQLException e) {
			return null;
		}
	}

	/**
	 * Static method used to clear the database of all data, and then delete them.
	 */
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
	 * Method to grant permissions to the public.
	 */
	public static void grantPublicRights() {
		try {
			Connection conn = createConnection("me", "password");
			String[] tables = {"accounting_employee", "address", "billing",
					"charge", "customer", "location", "package", "package_employee", "priority",
					"rate", "service", "transaction", "user", "zip_code"};
			for (String table : tables) {
				String query = String.format("GRANT all ON %s TO PUBLIC;", table);
				createAndExecute(conn, query);
			}
			closeConnection(conn);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Starts and runs the database.
	 *
	 * @param args: not used but you can use them
	 */
	public static void main(String[] args) {
		// If the database got corrupted, set this to true to wipe it out
		boolean clearDatabase = false;
		if (clearDatabase) {
			H2Access.clearDatabase();
		} else {
			// If starting from an empty slate, set this to true
			// NOTE: Set to true IF AND ONLY IF this is the first time running the code.
			boolean firstTime = false;
			if (firstTime) {
				CreateNewDatabase.initDatabase();
			} else {
				// After the first time run, this MUST be run to set the correct permissions
				// Should an error occur, rerun the permission again.
				boolean permissionsRun = false;
				if (permissionsRun) {
					CreateNewDatabase.permissions();
					grantPublicRights();
				}
			}
		}
	}
}
