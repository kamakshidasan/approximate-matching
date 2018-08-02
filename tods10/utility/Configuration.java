/*
 * Stores some constants, for example about the database used.
 * Helps to set up the experiments in an other environment.
 * 
 * Created on 12-Apr-06
 */
package utility;

import java.sql.Connection;

import sqltools.InsertBuffer;

import mysqltools.MySqlInsertBuffer;
import mysqltools.MySqlTools;

public class Configuration {

	public final static String CONFIG_FILE = "config.txt";
	public final static String DB_HOST = "default.host";
	public final static String DB_NAME = "default.dbname";
	public final static String DB_USER = "default.user";
	public final static String DB_PWD = "default.password";
	
	private String dbHost;
	private String dbName;
	private String dbUser;
	private String dbPwd;
	
	public static InsertBuffer getInsertBuffer() {
		return new MySqlInsertBuffer();
	}
	
	/**
	 * No instances of this class.
	 *
	 */
	public Configuration(String configfile) {
		// read parameters from CONFIG_FILE
		PropertyUtility.init(configfile, 
				"Configuration file '" + configfile + "' not found.\n"); 

		PropertyUtility prop = PropertyUtility.getInstance();
		if (prop == null) { System.out.println("prop is NULL"); }
		dbHost = prop.getProperty("host", DB_HOST);
		dbName = prop.getProperty("db", DB_NAME);
		dbUser = prop.getProperty("user", DB_USER);
		dbPwd = prop.getProperty("pwd", DB_PWD);
	}

	public static void loadDrivers() throws Exception {
		// MYSQL specific setup of test environment
		MySqlTools.loadDriver();
	}

	public Connection getConnection() throws Exception {
		// MYSQL specific setup of test environment
		return MySqlTools.getConnection(dbHost, dbName, dbUser, dbPwd);
	}
	
	public Connection[] getConnections(int num) throws Exception {
		Connection[] cons = new Connection[num];
		for (int i = 0; i < num; i++) {
			cons[i] = getConnection();
		}
		return cons;
	}
			
}
