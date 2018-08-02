package intervalenc;

import java.sql.SQLException;

public interface Cursor {

	public abstract boolean isAfterLast() throws SQLException;

	public abstract boolean next() throws SQLException;

	public abstract IntervalEncNode fetchNode() throws SQLException;

}