package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.io.*;

/**
 * Reads parse trees either from standard input or a specified file,
 * converting them to sentences and printing those sentences on standard
 * output.
 */
public class ParseToSentence {
  private static final int bufSize = Constants.defaultFileBufsize;

  private ParseToSentence() {}

  /**
   * Reads parse trees either from standard input or a specified file,
   * converting them to sentences and printing those sentences on standard
   * output.
   * <pre>usage: [filename]</pre>
   * where standard input is assumed if no argument is present.
   */
  public static void main(String[] args) {
    InputStream inStream = System.in;
    if (args.length > 0) {
      try {
	inStream = new FileInputStream(args[0]);
      } catch (FileNotFoundException fnfe) {
	System.err.println(fnfe);
	System.exit(-1);
      }
    }
    try {
      SexpTokenizer tok = new SexpTokenizer(inStream, Language.encoding(), bufSize);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null)
        System.out.println(Util.collectLeaves(curr));
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }
}
