package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.io.*;
import java.util.HashSet;

/**
 * Reads in a file of gold-standard parsed sentences and a file of
 * machine-parsed sentences, replacing every occurrence of
 * <tt>null</tt> in the machine-parsed file with the original
 * sentence and then adding fake parts of speech for each word of that
 * original sentence that will not to be deleted by the scorer.
 */
public class AddFakePos {

  // constants
  private final static Symbol nullSym = Symbol.add("null");
  private final static Symbol fakePos = Symbol.add("foo");

  // static data members
  private static HashSet posExceptions = new HashSet();
  static {
    String[] exceptions = {",", ":", "``", "''", "."};
    for (int i = 0; i < exceptions.length; i++)
      posExceptions.add(Symbol.add(exceptions[i]));
  }
  private static SexpList preterms = new SexpList(200);
  private static SexpList goldWords = new SexpList(200);

  private AddFakePos() {}

  private final static void usage() {
    System.err.println("usage: <gold parse file> [- | filename]");
    System.exit(1);
  }

  /**
   * Reads in parse trees either from a specified file or from standard input
   * and adds fake parts of speech to raw (un-parsed) sentences.
   * <pre>usage: &lt;gold parse file&gt; [- | &lt;filename&gt;]</pre>
   * where specifying <tt>-</tt> or using no arguments at all indicates to
   * read the parser output from standard input.
   */
  public static void main(String[] args) {
    InputStream in = System.in;
    InputStream goldIn = null;
    if (args.length < 1)
      usage();
    else {
      File goldFile = new File(args[0]);
      try { goldIn = new FileInputStream(goldFile); }
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
    SexpTokenizer goldTok = null;
    String enc = System.getProperty("file.encoding");
    try {
      tok = new SexpTokenizer(in, enc, 8192);
      goldTok = new SexpTokenizer(goldIn, enc, 8192);
    }
    catch (UnsupportedEncodingException uee) {
      System.err.println("error: encoding \"" + enc + "\" not supported");
      System.exit(1);
    }

    Sexp curr = null, goldCurr = null;
    try {
      while ((curr = Sexp.read(tok)) != null) {
        goldCurr = Sexp.read(goldTok);
	goldCurr = Language.training().removeNullElements(goldCurr);
	preterms.clear();
	goldWords.clear();
	collectPreterms(goldCurr);
        if (goldCurr == null) {
          System.err.println("error: ran out of sentences in gold file!");
          break;
        }
        if (curr == nullSym)
          curr = goldWords;
        addFakePos(curr);
        System.out.println(curr);
      }
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  private static void collectPreterms(Sexp gold) {
    if (Language.treebank().isPreterminal(gold)) {
      preterms.add(gold);
      Word word = Language.treebank().makeWord(gold);
      goldWords.add(word.word());
    }
    else if (gold.isList()) {
      SexpList goldTree = gold.list();
      int goldTreeLen = goldTree.length();
      for (int i = 0; i < goldTreeLen; i++)
	collectPreterms(goldTree.get(i));
    }
  }

  private static void addFakePos(Sexp sent) {
    if (sent.isList() && sent.list().isAllSymbols()) {
      SexpList sentList = sent.list();
      int sentLen = sentList.length();
      for (int i = 0; i < sentLen; i++) {
	Sexp word = sentList.get(i);
	SexpList preterm = preterms.listAt(i);
	Sexp pos = isException(preterm) ? preterm.get(0) : fakePos;
        SexpList wordList = new SexpList(2).add(pos).add(word);
        sentList.set(i, wordList);
      }
    }
  }
  private final static boolean isException(SexpList preterm) {
    return posExceptions.contains(preterm.get(0));
  }
}
