/*
 * Created on Sep 29, 2005
 */
package index;

import java.util.LinkedList;
import java.util.ListIterator;

/**
 * @author augsten
 */
public class EditLog {
	
	LinkedList log;
	
	public EditLog() {
		log = new LinkedList();
	}
	
	public EditLog(EditOperation editOperation) {
		this();
		this.push(editOperation);		
	}
	
	public void push(EditOperation editOperation) {
		log.addLast(editOperation);
	}
	
	public EditOperation pop() {
		return (EditOperation)log.removeLast();
	}
	
	public EditOperation getLast() {
		return (EditOperation)log.getLast();
	}
	
	public EditOperation getFirst() {
		return (EditOperation)log.getFirst();
	}

	/**
	 * listStartLast().previous() will return the first element in the log
	 * 
	 * @return listIterator
	 */
	public ListIterator listStartFirst() {
		return log.listIterator();
	}

	/**
	 * listStartLast().previous() will return the last element in the log
	 * 
	 * @return listIterator
	 */
	public ListIterator listStartLast() {
		return log.listIterator(log.size());
	}
	
	public boolean isEmpty() {
		return (log.isEmpty());
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		for (ListIterator it = listStartFirst(); it.hasNext();) {
			sb.append((it.next()) + "\n");
		}
		return sb.toString();
	}
	
}
