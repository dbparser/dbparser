package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.util.*;
import danbikel.parser.*;
import java.io.*;
import java.util.*;

/**
 * A class with a <tt>main</tt> method for reading trees and printing the
 * complete set of part-of-speech tags (preterminals) used in those trees.
 *
 * @see #main(String[])
 */
public class PrintTags {
  private static final int bufSize = Constants.defaultFileBufsize;

  private PrintTags() {}

  private static String[] usageMsg = {
    "usage: [-v|-help|-usage] [filename]",
    "where",
    "\t-v|-help|-usage: prints out this message",
    "\tfilename is the file to be processed (standard input is assumed if",
    "\t\tthis argument is \"-\" or is not present)"
  };

  private static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
  }

  /**
   * Reads in parse trees either from a specified file or from standard input,
   * collects all nonterminals and then prints them, one nonterminal per line,
   * to standard output.  By default, tags are not considered nonterminals.
   * <pre>usage: [- | <filename>] [-tags]</pre>
   * where specifying <tt>-</tt> or using no arguments at all indicates to
   * read from standard input, and where specifying -tags indicates to consider
   * part of speech tags to be nonterminals.
   */
  public static void main(String[] args) {
    InputStream inStream = System.in;
    String inFile = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-help") || args[i].equals("-usage") ||
          args[i].equals("-v")) {
        usage();
        return;
      }
      else if (!args[i].equals("-"))
        inFile = args[i];
    }
    if (inFile != null) {
      try {
	inStream = new FileInputStream(inFile);
      } catch (FileNotFoundException fnfe) {
	System.err.println(fnfe);
	System.exit(-1);
      }
    }
    try {
      CountsTable tagCounts = new CountsTableImpl();
      SexpTokenizer tok =
        new SexpTokenizer(inStream, Language.encoding(), bufSize);
      Sexp curr = null;
      int sentNum;
      for (sentNum = 0; (curr = Sexp.read(tok)) != null; sentNum++)
        Util.collectTags(tagCounts, curr);
      System.err.println("number of sentences processed: " + sentNum);
      Iterator it = tagCounts.entrySet().iterator();
      while (it.hasNext()) {
        MapToPrimitive.Entry entry = (MapToPrimitive.Entry)it.next();
        System.out.println(entry.getKey() + "\t" + entry.getDoubleValue());
      }
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }
}