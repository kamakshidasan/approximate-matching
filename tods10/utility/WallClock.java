/*
 * Created on 13-Apr-06
 */
package utility;

import java.io.PrintStream;

public class WallClock {

	long start;
	PrintStream out;
	
	/**
	 * By default output goes to System.err.
	 */
	public WallClock() {
		start = 0;
		out = System.err;
	}
	
	/**
	 * 
	 * @param out where to print messages
	 */	
	public WallClock(PrintStream out) {
		this();
		this.out = out;
	}

	/**
	 * Print a message and start clock. 
	 * 
	 * @param msg
	 */
	public void start(String msg) {
		out.print(msg);
		start = System.currentTimeMillis();		
	}
	
	/**
	 * Return time and reset.
	 * 
	 * @return
	 */
	public long stop() {
		long time = System.currentTimeMillis() - start;
		start = System.currentTimeMillis();
		return time;
	}
	
	/**
	 * Return time without resetting.
	 * 
	 * @	return
	 */
	public long getTime() {
		return System.currentTimeMillis() - start;
	}
	
	/**
	 * Print the time in millisecs without resetting.
	 * 
	 * @param msg
	 * @return
	 */
	public void printTime() {
		out.println(getTime() + "ms");
	}

	/**
	 * Println the time in millisecs and reset.
	 * 
	 * @param msg
	 * @return
	 */
	public long printStop() {
		long time = stop();
		out.println(time + "ms");
		return time;
	}	
	
}
