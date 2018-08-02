/*
 * used in the following experiments:
 * - thresholdjoin
 */
/*
 * Created on Aug 8, 2005
 */

package executable;
import java.sql.Connection;

import utility.Configuration;
import xml.AdjacencyEncHandlerLbl;
import xml.IntervalEncHandlerLbl;
import xml.IntervalEncHandler;
import adjacencyenc.AdjacencyEncForest;
import intervalenc.IntervalEncForest;

/**
 * @author augsten
 */
public class LoadXMLForest {
	
	public static void writeErrMsg() {
		System.err.println("\nparams: table_name {adj|ie|ieLbl} file_1 ... file_n\n");
		System.err.println("  adj    ... adjacency list encoding with (id,label) nodes");
		System.err.println("  ie    ... interval encoding with (id,label,value) nodes");
		System.err.println("  ieLbl   ... interval encoding with (id, label) nodes");
		System.err.println("  file_i ... XML file, will get ID=i in the table\n");
		System.err.println("NOTE:\n  For large XML documents you might have to set a larger value\n  for the entity expantion limit, e.g.,");
		System.err.println("\n  java -DentityExpansionLimit=20000000 LoadXMLForest ...\n");
		System.exit(-1);
	}
	
	public static void main(String[] args) throws Exception {
		
		Configuration config = new Configuration(Configuration.CONFIG_FILE);
		Configuration.loadDrivers();
		if (args.length < 3) {
			LoadXMLForest.writeErrMsg();
		}
		
		String tblName = args[0];
		String encoding = args[1];
		
		// connect to database
		Connection[] con = config.getConnections(2);
		
		try {
			long start = System.currentTimeMillis();
			if (encoding.equals("adj")) {
				// create table
				AdjacencyEncForest f = new AdjacencyEncForest(con[0], con[1],
						Configuration.getInsertBuffer(), tblName);
				f.reset();
				
				// lead files
				for (int i = 2; i < args.length; i++) {
					long start1 = System.currentTimeMillis();
					System.out.print("Loading from file '" + args[i] + "'...");
					AdjacencyEncHandlerLbl.parseFromFile(args[i], f, i - 1, true, true);
					System.out.println((System.currentTimeMillis() - start1) + "ms");
				}
				f.createIndices();
			} else if (encoding.equals("ieLbl")) {
				IntervalEncForest f = new IntervalEncForest(con[0], con[1], 
						Configuration.getInsertBuffer(), tblName);
				f.reset();
				
				long start1;
				for (int i = 2; i < args.length; i++) {
					start1 = System.currentTimeMillis();
					System.out.print("Loading from file '" + args[i] + "'...");
					IntervalEncHandlerLbl.parseFromFile(args[i], f, i - 1, true, true);
					System.out.println((System.currentTimeMillis() - start1) + "ms");
				}				
				start1 = System.currentTimeMillis();
				System.out.print("Building index on (treeID, lft)...");
				f.createPreorderIndex();
				System.out.println((System.currentTimeMillis() - start1) + "ms");
			} else if (encoding.equals("ie")) {
				IntervalEncForest f = new IntervalEncForest(con[0], con[1], 
						Configuration.getInsertBuffer(), tblName);
				f.reset();
				
				long start1;
				for (int i = 2; i < args.length; i++) {
					start1 = System.currentTimeMillis();
					System.out.print("Loading from file '" + args[i] + "'...");
					IntervalEncHandler.parseFromFile(args[i], f, i - 1, false, true);
					System.out.println((System.currentTimeMillis() - start1) + "ms");
				}				
				start1 = System.currentTimeMillis();
				System.out.print("Building index on (treeID, lft)...");
				f.createPreorderIndex();
				System.out.println((System.currentTimeMillis() - start1) + "ms");
			} else {
				LoadXMLForest.writeErrMsg();
			}
			System.out.println("Loaded " + (args.length - 2) + " files in " + (System.currentTimeMillis() - start) + "ms.");
		} catch (Exception e) {
			System.err.println("Entity expansion limit to small for your XML documents. Start the programm with a larger limit.");
		}
	}
}
