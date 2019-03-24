package cs320.model;
import java.sql.*;

/**
 * This is a sample main program. 
 * You will create something similar
 * to run your database.
 * 
 * @author wps
 *
 */
public class H2Access {
	private String dbLocation = "./h2demo/h2demo";

	public String getDbLocation() {
		return dbLocation;
	}

	public void setDbLocation(String dbLocation) {
		this.dbLocation = dbLocation;
	}

	/**
	 * Creates and returns a database connection with the given params. Returns None
	 * if the connection fails, relies on any calling function to handle. After the
	 * connection has been used, should be closed with closeConnection()
	 * @param user: user name for the owner of the database
	 * @param password: password of the database owner
	 */
	public Connection createConnection(String user, String password){
		try {

			//This needs to be on the front of your location
			String url = "jdbc:h2:" + this.dbLocation;
			//This tells it to use the h2 driver
			Class.forName("org.h2.Driver");
			
			//creates the connection
			return DriverManager.getConnection(url, user, password);
		} catch (SQLException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void closeConnection(Connection conn){
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public ResultSet createAndExecuteQuery(Connection conn, String query){
		try {
			Statement stmt = conn.createStatement();
			return stmt.executeQuery(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void createAndExecute(Connection conn, String query){
		try {
			Statement stmt = conn.createStatement();
			stmt.execute(query);
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Starts and runs the database
	 * @param args: not used but you can use them
	 */
	public static void main(String[] args) {
		CreateNewDatabase cDb = new CreateNewDatabase();
		//try{cDb.accountingEmployees();} catch (SQLException e){e.printStackTrace();}

		System.out.println("\nSample Package Employee updating a packages location.");
		try (EmployeeAccess employee1 = new EmployeeAccess("FQAX", "password", "packageEmployee")) {
			System.out.println("User ID: " + employee1.getId());
			employee1.updatePackageLocation("TOT0MYPN2PLK", 131, "B9IWEA");
		}

		try (EmployeeAccess employee2 = new EmployeeAccess("KENL", "password", "accountingEmployee")) {
			System.out.println("\nSample Accounting Employee looking up customers named Amy.");
			ResultSet results = employee2.getCustomersWhere("first_name='Amy'");
			while(results.next()){
				System.out.printf("Person %d: %d %d %d %s %s %s\n",
						results.getInt(1),
						results.getInt(2),
						results.getInt(3),
						results.getInt(4),
						results.getString(5),
						results.getString(6),
						results.getString(7));
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
				System.out.printf("Package Date %d: %d %s %f %f %f %f %s \n",
						results.getInt(1),
						results.getInt(2),
						results.getString(3),
						results.getDouble(4),
						results.getDouble(5),
						results.getDouble(6),
						results.getDouble(7),
						results.getString(8));
			}
		} catch (SQLException e){
			e.printStackTrace();
		}
	}


}
