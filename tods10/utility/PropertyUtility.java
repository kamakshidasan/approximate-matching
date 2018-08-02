package utility;


import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;


/**
 * <p>
 * The <code>PropertyUtility</code> class is used to read parameters ("properties") from a file. 
 * The parameters in the file can also be overwritten with the -D option in java. 
 * <p>
 * Example:<br>
 * <code>java -Dhost=rose.inf.unibz.it RunProgram</code><br>
 * calls "RunProgram" and sets the property "host" to "rose.inf.unibz.it".
 * <p>
 *  <a href="http://www.inf.unibz.it/dis">Database Information Systems - Research Group</a>
 * <p>
 * Free University of Bozen-Bolzano, Italy
 * </p>
 * </p>
 * 
 * @author <a href="mailto:markus.innerebner@inf.unibz.it">Markus Innerebner</a>
 * @version 1.0
 */
public class PropertyUtility {

	private static PropertyUtility instance = null;
	private Properties properties = new Properties();

	/**
	 * 
	 * @param fileName name of the configuration file
	 * @param fileNotFoundMsg print this message if the configuration file can not be found
	 */
	private PropertyUtility(String fileName, String fileNotFoundMsg) {
		FileInputStream inputStream;
		try {
			inputStream = new FileInputStream(fileName);
			properties.load(inputStream);
		} catch (FileNotFoundException e) {
			System.err.print(fileNotFoundMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * 
	 * @param fileName name of the configuration file
	 * @param fileNotFoundMsg print this message if the configuration file can not be found
	 */
	private PropertyUtility(String fileName) throws IOException {
		FileInputStream inputStream;
		inputStream = new FileInputStream(fileName);
		properties.load(inputStream);
	}

	/**
	 * <p>
	 * Method get
	 * </p>
	 * 
	 * @return the Marshaller as Singleton
	 */
	public static PropertyUtility getInstance() {
		return instance;
	}
	
	/**
	 * 
	 * @param fileName name of the configuration file
	 * @param fileNotFoundMsg print this message if the configuration file can not be found
	 */
	public static void  init(String path) throws IOException {
		if(instance == null) {
			instance = new PropertyUtility(path);
			instance.properties.putAll(System.getProperties());
		}
	}

	/**
	 * 
	 * @param fileName name of the configuration file
	 * @param fileNotFoundMsg print this message if the configuration file can not be found
	 */
	public static void  init(String path, String fileNotFoundMsg) {
		if(instance == null) {
			instance = new PropertyUtility(path, fileNotFoundMsg);
			instance.properties.putAll(System.getProperties());
		}
	}

	/**
	 * <p>
	 * Method getProperty
	 * </p>
	 * 
	 * @param property the property from which getting the value
	 * @return the value of the property or "null" if no corresponding property was found
	 */
	public String getProperty(String property) {
		return properties.getProperty(property, null);
	}
	

	/**
	 * 
	 * @param property property the property from which getting the value
	 * @param defaultValue
	 * @return
	 */
	public String getProperty(String property, String defaultValue) {
		return properties.getProperty(property, defaultValue);
	}

}
