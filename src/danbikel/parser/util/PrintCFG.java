package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.io.*;
import java.util.*;

public class PrintCFG {
  private static final int bufSize = Constants.defaultFileBufsize;
  private static final Symbol POS = Symbol.add("POS");

  private static Treebank treebank = Language.treebank();

  private PrintCFG() {}

  private static String[] usageMsg = {
    "usage: [-v|-help|-usage] [-ct | --convert-tags] [filename]",
    "where",
    "\t-v|-help|-usage: prints out this message",
    "\t-ct|--convert-tags: converts all part-of-speech tags to POS",
    "\tfilename is the file to be processed (standard input is assumed if",
    "\t\tthis argument is \"-\" or is not present)"
  };

  private static void usage() {
    for (int i = 0; i < usageMsg.length; i++)
      System.err.println(usageMsg[i]);
  }

  private static void printCFGExpansions(Writer writer,
					 Sexp tree, boolean convertTags)
      throws IOException {
    if (convertTags)
      tree = convertTags(tree);
    printCFGExpansions(writer, tree);
  }

  private static Sexp convertTags(Sexp tree) {
    if (treebank.isPreterminal(tree)) {
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 1; i < treeListLen; i++) {
	Sexp currChild = treeList.get(i);
	if (treebank.isPreterminal(currChild)) {
	  Word word = treebank.makeWord(currChild);
	  word.setTag(POS);
	  treeList.set(i, treebank.constructPreterminal(word));
	}
	else
	  convertTags(currChild);
      }
    }
    return tree;
  }

  private static void printCFGExpansions(Writer writer, Sexp tree)
      throws IOException {
    if (treebank.isPreterminal(tree)) {
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      Symbol lhs = treeList.symbolAt(0);
      writer.write(lhs.toString());
      writer.write(" -> ");
      for (int i = 1; i < treeListLen; i++) {
	writer.write(treeList.getChildLabel(i).toString());
	if (i < treeListLen - 1)
	  writer.write(" ");
      }
      writer.write("\n");
      for (int i = 1; i < treeListLen; i++)
	printCFGExpansions(writer, treeList.get(i));
    }
  }

  /**
   * Reads in parse trees either from a specified file or from standard input,
   * collects all CFG expansions and prints them, one per line, in the form
   * <tt>LHS -> RHS</tt>, to standard output.
   * <pre>usage: [- | <filename>] [-ct | --convert-tags]</pre>
   */
  public static void main(String[] args) {
    boolean convertTags = false;
    InputStream inStream = System.in;
    String inFile = null;
    for (int i = 0; i < args.length; i++) {
      if (args[i].equals("-help") || args[i].equals("-usage") ||
	  args[i].equals("-v")) {
	usage();
	return;
      }
      else if (args[i].equals("-ct") || args[i].equals("--convert-tags")) {
	convertTags = true;
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
      SexpTokenizer tok =
	new SexpTokenizer(inStream, Language.encoding(), bufSize);
      Sexp curr = null;
      OutputStreamWriter outStreamWriter =
	new OutputStreamWriter(System.out, Language.encoding());
      Writer writer = new BufferedWriter(outStreamWriter, bufSize);
      int sentNum;
      for (sentNum = 0; (curr = Sexp.read(tok)) != null; sentNum++)
	printCFGExpansions(writer, curr, convertTags);
      writer.flush();
      System.err.println("number of sentences processed: " + sentNum);
    }
    catch (Exception e) {
      System.out.println(e);
    }
  }
}
