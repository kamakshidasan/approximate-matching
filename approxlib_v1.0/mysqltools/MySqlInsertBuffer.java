/**
 * You give it a connection and a size, then you can use it to insert many tuples in a database without 
 * exceeding the maximum allowed query size.
 * Uses MySQL Syntax!
 */

package mysqltools;
import java.sql.*;

import sqltools.InsertBuffer;

public class MySqlInsertBuffer extends InsertBuffer{

	public static final int MAX_BUFFER_SIZE = 10000;
	
	/**
	 *  Creates an MySqlInsertBuffer, that has still to be open.
	 */
	public MySqlInsertBuffer() {
		open = false;
	}
		
	@Override
	public int getDefaultSize(Connection con) throws SQLException {		
		return Math.min(MySqlTools.max_allowed_packet(con), MAX_BUFFER_SIZE);
	}
	
	@Override
	public void insert(String line) throws SQLException  {
		if (open) {
			int len = line.length() + 10;
			if (qry.length() + len >= qry.capacity()) {
				flush();
			}
			if (!empty) {
				qry.append(",");
			}
			empty = false;
			qry.append(line);
		}
	}
		
}