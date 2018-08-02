package matching;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.LinkedList;

import util.FormatUtilities;

public class MatchIO {
	
	public static void toFile(Match[] matches, String filename, char separator, String comment) throws IOException {
		BufferedWriter out = 
			new BufferedWriter(new FileWriter(filename));
		
		String outline;
		if (comment != null) {
			outline = "#" + comment + "\n";
		} else {
			outline = "# id1" + separator + "id2\n";
		}
		out.write(outline, 0, outline.length());
		
		for (int i = 0; i < matches.length; i++) {
			outline = matches[i].getIdRow() + "" + separator + "" + matches[i].getIdCol() + "\n";
			out.write(outline, 0, outline.length());			
		}
		out.close();
	}
	
	public static Match[] fromFile(String filename, char separator)  throws IOException {
		LineNumberReader in = 
			new LineNumberReader(new FileReader(filename));
		
		String inline;
		LinkedList<Match> matchesList = new LinkedList<Match>();
		while ((inline = in.readLine()) != null) {
			if (!(inline.charAt(0) == '#')) {
				String[] fields = FormatUtilities.getFields(inline, separator);
				int idRow = Integer.parseInt(fields[0]);
				int idCol = Integer.parseInt(fields[1]);
				matchesList.add(new Match(idRow, idCol));
			}
		}
		in.close();
		return matchesList.toArray(new Match[matchesList.size()]);
	}
}
