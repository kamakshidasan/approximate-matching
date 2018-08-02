/*
 * Created on Sep 28, 2005
 */
package index;

import java.sql.SQLException;

/**
 * Any edit operation that is supported by the the {@link index.Editable} interface.
 * 
 * @author augsten
 */
abstract public class EditOperation {
	abstract public void applyTo(Editable editable) throws SQLException;	
	abstract public EditOperation reverseEditOp();
	@Override
	abstract public String toString();
}