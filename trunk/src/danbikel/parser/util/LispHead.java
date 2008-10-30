package danbikel.parser.util;

import danbikel.lisp.*;
import java.io.*;

/**
 * Equivalent of Unix <tt>head</tt> command but for input streams/files
 * containing S-expressions.  The S-expressions are read from standard input
 * or from a file specified on the command-line and are printed out, one
 * line per S-expression.  If no integer flag is present, the first 10
 * S-expressions will be printed; otherwise, the specified number of
 * S-epxressions will be printed.
 */
public class LispHead {
  private static final int bufSize = 8192;

  private LispHead() {}

  private static void usage() {
    System.err.println("usage: [-i] [-number | -n number ] [ filename ]");
  }

  /**
   * Prints out the first <tt>n</tt> S-expressions from a file or standard
   * input.  Using the <tt>-i</tt> option indicates that the tree file being
   * read is in IBM format.
   * <pre>usage: [-i] [-number | -n number ] [ filename ]</pre>
   */
  public static void main(String[] args) {
    boolean ibmFormat = false;
    int numSexps = 10;
    String numSexpsStr = null;
    InputStream inStream = System.in;
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
	if (args[i].equals("-n")) {
	  if ((i + 1) == args.length) {
	    usage();
	    System.exit(-1);
	  }
	  else
	    numSexpsStr = args[++i];
	}
        else if (args[i].equals("-i")) {
          ibmFormat = true;
        }
        else
	  numSexpsStr = args[i].substring(1);
      }
      else
	try {
	  inStream = new FileInputStream(args[i]);
	} catch (FileNotFoundException fnfe) {
	  System.err.println(fnfe);
	  System.exit(-1);
	}
    }
    if (numSexpsStr != null) {
      try {
	numSexps = Integer.parseInt(numSexpsStr);
      } catch (NumberFormatException nfe) {
	usage();
	System.exit(-1);
      }
    }
    String enc = System.getProperty("file.encoding");
    try {
      OutputStreamWriter outStreamWriter =
        new OutputStreamWriter(System.out, enc);
      PrintWriter out =
        new PrintWriter(new BufferedWriter(outStreamWriter, bufSize));
      SexpTokenizer tok = ibmFormat ?
                          Util.ibmTokenizer(newReader(inStream, enc), true) :
                          new SexpTokenizer(inStream, enc, bufSize);
      Sexp curr = null;
      try {
        while (numSexps-- > 0 && (curr = readTree(tok, ibmFormat)) != null) {
  	out.println(curr.toString());
        }
      } catch (IOException ioe) { System.err.println(ioe); }
      out.flush();
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println(uee);
    }
  }

  private static BufferedReader newReader(InputStream inStream, String enc)
    throws UnsupportedEncodingException {
    return new BufferedReader(new InputStreamReader(inStream, enc), bufSize);
  }

  private static Sexp readTree(SexpTokenizer tok, boolean ibmFormat)
    throws IOException {
    return ibmFormat ? Util.readIbmTree(tok) : Sexp.read(tok);
  }
}
