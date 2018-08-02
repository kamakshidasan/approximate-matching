/**
 * 
 */
package tasmTED;



/**
 * @author Nikolaus Augsten
 *
 */
public class XMLPostorderSource implements PostorderSource {

	private String filename;
	private boolean validating, nameSpaceAware, omitEmptyText;

	/**
	 * @param filename
	 * @param validating
	 * @param nameSpaceAware
	 * @param omitEmptyText
	 */
	public XMLPostorderSource(String filename, boolean validating, boolean nameSpaceAware, boolean omitEmptyText) {
		super();
		this.filename = filename;
		this.validating = validating;
		this.nameSpaceAware = nameSpaceAware;
		this.omitEmptyText = omitEmptyText;
	}

	/* (non-Javadoc)
	 * @see tasmTED.PostorderSource#appendTo(tasmTED.PostorderQueue)
	 */
	public void appendTo(PostorderQueue postorderQueue) {
		PostorderQueueHandler.parseFromFile(filename,
				validating, nameSpaceAware, 
				omitEmptyText, postorderQueue);				
	}

}
