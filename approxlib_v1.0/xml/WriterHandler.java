/*
 * Created on Jul 22, 2008
 */
package xml;

import java.io.Writer;

import org.xml.sax.helpers.DefaultHandler;

abstract public class WriterHandler extends DefaultHandler {
	
	Writer outWriter;

	public Writer getOutWriter() {
		return outWriter;
	}

	public void setOutWriter(Writer outWriter) {
		this.outWriter = outWriter;
	}
	
	abstract public void reset();
	
}
