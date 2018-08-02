/*
 * Created on 13-Apr-06
 */
package intervalenc;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author augsten
 */
public class IntervalEncCursor implements Cursor {
	
	private ResultSet rs;
	
	private IntervalEncForest f;
	
	/**
	 * 
	 */
	public IntervalEncCursor(ResultSet rs, IntervalEncForest f) {
		this.rs = rs;
		this.f = f;
	}
	

	/* (non-Javadoc)
	 * @see intervalenc.Cursor#isAfterLast()
	 */
	public boolean isAfterLast() throws SQLException {
		return rs.isAfterLast();
	}
	
	/* (non-Javadoc)
	 * @see intervalenc.Cursor#next()
	 */
	public boolean next() throws SQLException {
		return rs.next();
	}
	
	/* (non-Javadoc)
	 * @see intervalenc.Cursor#fetchNode()
	 */
	public IntervalEncNode fetchNode() throws SQLException {
		IntervalEncNode node = 
			new IntervalEncNode(rs.getInt(f.getAtbTreeID()),       // treeID
					rs.getString(f.getAtbLabel()), // label
					rs.getString(f.getAtbValue()), // value					
					rs.getInt(f.getAtbLft()),       // left interval number
					rs.getInt(f.getAtbRgt()));      // right interval number
		return node;
	}

	/**
	 * Resets the cursor, so that you can use an other cursor on the table.
	 *
	 */
	public void close() throws SQLException {
		rs.close();
	}

}
