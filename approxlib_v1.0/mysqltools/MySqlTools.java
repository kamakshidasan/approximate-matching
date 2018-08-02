package mysqltools;

import java.sql.*;

import sqltools.SQLTools;
import sqltools.TableWrapper;

/**
 Test...
 */
public class MySqlTools {
	
	/**
	 *  Checks whether the MySQL JDBC Driver is installed and load it.
	 *
	 *  @throws Exception
	 */	
	public static void loadDriver() throws Exception {
		
		try {
			Class.forName ( "org.gjt.mm.mysql.Driver" );
			if (SQLTools.DEBUG) {
				System.err.println ( "MySQL JDBC Driver Loaded." );
			}
		} catch ( java.lang.ClassNotFoundException e ) {
			System.err.println("MySQL JDBC Driver not found ... ");
			throw ( e );
		}
	}
	
	/**
	 *  Returns a connection to the MySQL database.
	 * 
	 *  @param host hostname of mysql server to connect to
	 *  @param database database name to connect to
	 *  @param user user name
	 *  @param pwd password
	 *
	 *  @return the connection object ({@link java.sql.Connection java.sql.Connection})
	 *
	 *  @throws  Exception
	 */
	public static Connection getConnection (String host, String database, 
			String user, String pwd)
	throws Exception {
		
		String url = "";
		try {
			url = "jdbc:mysql://" + host + "/" + database;	
			Connection con = DriverManager.getConnection(url, user, pwd);
			if (SQLTools.DEBUG) {
				System.err.println("Connection established to " + 
						url + " for user '" + user + "'...");
			}
			return con;
		} catch ( java.sql.SQLException e ) {
			System.err.println("Connection couldn't be established to " + 
					url + " for user '"+ user + "'...");
			throw (e);
		}
	}
	
	
	public static int max_allowed_packet(Connection con) throws SQLException {
		Statement s = con.createStatement();
		ResultSet rs = s.executeQuery("SHOW VARIABLES LIKE 'max_allowed_packet'");
		rs.next();		
		return rs.getInt("Value");	
	}
	
	public static void addTblComment(TableWrapper tw, String comment) throws SQLException {
		String qry = "ALTER TABLE `" + tw.getTblName()  
			+ "` COMMENT = '" + SQLTools.escapeSingleQuote(comment) + "'";
		SQLTools.execute(tw.getStatement(), qry, 
				"Adding comment to table '" + tw.getTblName() + "'");
	}
	
}
