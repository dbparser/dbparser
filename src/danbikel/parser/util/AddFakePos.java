package danbikel.parser.util;

import danbikel.lisp.*;
import java.io.*;

/**
 * Reads in a file of unparsed sentences and a file of parsed sentences,
 * replacing every occurrence of <tt>null</code> in the parsed file with
 * the original sentence and then adding fake parts of speech for each word
 * of that original sentence.
 */
public class AddFakePos {

  private final static Symbol nullSym = Symbol.add("null");

  private AddFakePos() {}

  private final static void usage() {
    System.err.println("usage: <orig. sentence file> [- | filename]");
    System.exit(1);
  }

  /**
   * Reads in parse trees either from a specified file or from standard input
   * and adds fake parts of speech to raw (un-parsed) sentences.
   * <pre>usage: <orig. sentence file> [- | <filename>]</pre>
   * where specifying <tt>-</tt> or using no arguments at all indicates to
   * read the parser output from standard input.
   */
  public static void main(String[] args) {
    InputStream in = System.in;
    InputStream origIn = null;
    if (args.length < 1)
      usage();
    else {
      File origFile = new File(args[0]);
      try { origIn = new FileInputStream(origFile); }
      catch (FileNotFoundException fnfe) {
        System.err.println("error: file \"" + args[0] + "\" does not exist");
        System.exit(1);
      }
    }
    if (args.length > 1) {
      if (!args[1].equals("-")) {
        File file = new File(args[1]);
        try { in = new FileInputStream(file); }
        catch (FileNotFoundException fnfe) {
          System.err.println("error: file \"" + args[1] + "\" does not exist");
          System.exit(1);
        }
      }
    }
    BufferedReader br = new BufferedReader(new InputStreamReader(in));
    SexpTokenizer tok = null;
    SexpTokenizer origTok = null;
    String enc = System.getProperty("file.encoding");
    try {
      tok = new SexpTokenizer(in, enc, 8192);
      origTok = new SexpTokenizer(origIn, enc, 8192);
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println("error: encoding \"" + enc + "\" not supported");
      System.exit(1);
    }

    Sexp curr = null, origCurr = null;
    try {
      while ((curr = Sexp.read(tok)) != null) {
        origCurr = Sexp.read(origTok);
        if (origCurr == null) {
          System.err.println("error: ran out of sentences in original " +
                             "sentence file!");
          break;
        }
        if (curr == nullSym)
          curr = origCurr;
        addFakePos(curr);
        System.out.println(curr);
      }
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  private static Symbol fakePos = Symbol.add("foo");

  private static void addFakePos(Sexp sent) {
    if (sent.isList() && sent.list().isAllSymbols()) {
      SexpList sentList = sent.list();
      int sentLen = sentList.length();
      for (int i = 0; i < sentLen; i++) {
        SexpList wordList = new SexpList(2).add(fakePos).add(sentList.get(i));
        sentList.set(i, wordList);
      }
    }
  }
}