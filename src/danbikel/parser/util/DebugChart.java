package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.io.*;
import java.util.*;

public class DebugChart {
  // data member
  private static Symbol traceTag = Language.training().traceTag();

  private DebugChart() {}

  /**
   * Prints out to <code>System.err</code> which constituents of the specified
   * gold-standard parse tree were found by the parser, according to its output
   * chart file.  The specified filename must point to a valid Java object file
   * that contains two objects: a <code>Chart</code> object and a
   * <code>SexpList</code> object (in that order), where the
   * <code>SexpList</code> object is the list of the original words in the
   * parsed sentence (which is the original sentence, but with potentially
   * certain words removed after preprocessing).  This method is intended to be
   * used for off-line debugging (i.e., after a parsing run during which chart
   * object files were created).
   *
   * @param chartFilename the filename of a parser chart file, which is a
   * Java object file containing two serialized objects: a <code>Chart</code>
   * object and a <code>SexpList</code> object
   * @param goldTree the gold-standard parse tree, as found in the original
   * <tt>combined</tt> file directory of the Penn Treebank, except with its
   * outer parentheses removed
   */
  public static void findConstituents(String chartFilename,
				      Sexp goldTree) {
    try {
      ObjectInputStream ois =
	new ObjectInputStream(new FileInputStream(chartFilename));
      Chart chart = (Chart)ois.readObject();
      SexpList origWords = (SexpList)ois.readObject();
      Sexp newGoldTree = Language.training().preProcess(goldTree);
      System.err.println(Util.prettyPrint(newGoldTree));
      HeadTreeNode headTree = new HeadTreeNode(newGoldTree);
      String downcaseWordsStr = Settings.get(Settings.downcaseWords);
      if (Boolean.valueOf(downcaseWordsStr).booleanValue())
	downcaseWords(headTree);
      findConstituents(chart, headTree);
    }
    catch (FileNotFoundException fnfe) {
      System.err.println(fnfe);
    }
    catch (StreamCorruptedException sce) {
      System.err.println(sce);
    }
    catch (ClassNotFoundException cnfe) {
      System.err.println(cnfe);
    }
    catch (IOException ioe) {
      System.err.println(ioe);
    }
  }

  /**
   * Prints out to <code>System.err</code> which constituents of the specified
   * gold-standard parse tree were found by the parser, according to the
   * specified chart.  This method is intended to be used for on-line parser
   * debugging (that is, debugging during a decoding run).  The only functional
   * difference between this method and {@link #findConstituents(String,Sexp)}
   * is that this method does not print out the original words of the sentence,
   * as that can be separately accomplished during decoding by setting
   * <code>Decoder.debugInit</code> to <code>true</code>.
   *
   * @param chart the chart of the parser, after processing the sentence
   * to be analyzed
   * @param goldTree the gold-standard parse tree, as found in the original
   * <tt>combined</tt> directory of the Penn Treebank, except with its
   * outer parentheses removed
   */
  public static void findConstituents(Chart chart, Sexp goldTree) {
    Sexp newGoldTree = Language.training().preProcess(goldTree);
    HeadTreeNode headTree = new HeadTreeNode(newGoldTree);
    String downcaseWordsStr = Settings.get(Settings.downcaseWords);
    if (Boolean.valueOf(downcaseWordsStr).booleanValue())
      downcaseWords(headTree);
    findConstituents(chart, headTree);
  }

  // helper methods
  public static void findConstituents(Chart chart, HeadTreeNode tree) {
    if (!tree.isPreterminal()) {
      int start = tree.leftIdx();
      // head tree nodes specify right index as index of rightmost word PLUS 1,
      // but we simply want index of rightmost word
      int end = tree.rightIdx() - 1;
      Symbol label = (Symbol)tree.label();
      Iterator it = chart.get(start, end);
      CKYItem found = null;
      CKYItem foundNoStop = null;
      CKYItem unlexicalizedFound = null;
      CKYItem unlexicalizedFoundNoStop = null;
      while (it.hasNext()) {
	CKYItem item = (CKYItem)it.next();
	if (item.label().equals(label)) {
	  if (item.stop())
	    unlexicalizedFound = item;
	  else
	    unlexicalizedFoundNoStop = item;
	  if (item.headWord().equals(tree.headWord())) {
	    if (item.stop()) {
	      System.err.println("found " + itemToString(item));
	      found = item;
	      break;
	    }
	    else {
	      foundNoStop = item;
	    }
	  }
	}
       }

      if (found == null) {
	System.err.print("didn't find " + headTreeNodeToString(tree));
	boolean foundOther =
	  foundNoStop != null ||
	  unlexicalizedFound != null || unlexicalizedFoundNoStop != null;
	if (foundOther) {
	  System.err.println(" but found:" +
			     (unlexicalizedFound != null ?
			      "\n\t" + itemToString(unlexicalizedFound) : "") +
			     (foundNoStop != null ?
			      "\n\t" + itemToString(foundNoStop) : "") +
			     (unlexicalizedFound == null &&
			      unlexicalizedFoundNoStop != null ?
			      "\n\t" + itemToString(unlexicalizedFoundNoStop) :
			      ""));
	}
	else
	  System.err.println();
      }

      // recurse on head child
      findConstituents(chart, tree.headChild());
      // recurse on pre- and post-mods
      it = tree.preMods().iterator();
      while (it.hasNext())
	findConstituents(chart, (HeadTreeNode)it.next());
      it = tree.postMods().iterator();
      while (it.hasNext())
	findConstituents(chart, (HeadTreeNode)it.next());
    }
  }

  /**
   * Returns a string of the form
   * <tt>[start,end,label&lt;headWord&gt;, &lt;headChild&gt;]</tt>
   * where <tt>&lt;headWord&gt;</tt> is the head word and
   * where <tt>&lt;headChild&gt;</tt> is either a string of the form
   * <tt>[start,end,label]</tt>
   * or <tt>null</tt> if the specified <code>HeadTreeNode</code> is
   * a preterminal.
   *
   * @return a string representation of the specified node
   */
  public static String headTreeNodeToString(HeadTreeNode node) {
    return ("[" + node.leftIdx() + "," + (node.rightIdx() - 1) + "," +
	    node.label() + node.headWord() + ", " +
	    (node.isPreterminal() ? "[null]" :
	     "[" +
	     node.headChild().leftIdx() + "," +
	     (node.headChild().rightIdx() - 1) + "," +
	     node.headChild().label() +
	     "]") +
	    "]");
	    
  }

  public static String itemToString(CKYItem item) {
    return ("[" + item.start() + "," + item.end() + "," +
	    item.label() + item.headWord() + ",stop=" +
	    (item.stop() ? "t" : "f") + ", " +
	    (item.isPreterminal() ? "[null]" :
	     "[" +
	     item.headChild().start() + "," +
	     item.headChild().end() + "," +
	     item.headChild().label() +
	     "]") +
	    "]");
  }

  public static void downcaseWords(HeadTreeNode tree) {
    if (tree.isPreterminal()) {
      if (tree.headWord().tag() != traceTag) {
	Word headWord = tree.headWord();
	headWord.setOriginalWord(headWord.word());
	headWord.setWord(Symbol.add(headWord.word().toString().toLowerCase()));
      }
    }
    else {
      downcaseWords(tree.headChild());
      for (Iterator mods = tree.preMods().iterator(); mods.hasNext(); )
	downcaseWords((HeadTreeNode)mods.next());
      for (Iterator mods = tree.postMods().iterator(); mods.hasNext(); )
	downcaseWords((HeadTreeNode)mods.next());
    }
  }

  /**
   * Removes preterminals from the specified tree that are not found in
   * the specified list of words.
   *
   * @param words the words of the sentence that was parsed (meaning that
   * some of the words of the original sentence may have been pruned)
   * @param tree the parse tree whose preterminals are to match
   * <code>words</code>
   * @param wordIdx the threaded word index; to be <tt>0</tt> for all
   * non-recursive calls
   * @return the modified tree
   */
  public static Sexp removePreterms(SexpList words, Sexp tree, int wordIdx) {
    if (Language.treebank().isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 0; i < treeList.length(); i++) {
	Sexp currChild = treeList.get(i);
	if (Language.treebank().isPreterminal(currChild)) {
	  Word treeWord = Language.treebank().makeWord(currChild);
	  if (treeWord.word() == words.get(wordIdx))
	    wordIdx++;
	  else
	    treeList.remove(i--);
	}
	else
	  removePreterms(words, currChild, wordIdx);
      }
    }
    return tree;
  }

  /**
   * Removes interior nodes of the specified tree that are not preterminals and
   * that have no children.
   *
   * @param tree the tree from which to remove childless interior nodes
   * @return the modified tree
   */
  public static Sexp removeChildlessNodes(Sexp tree) {
    if (Language.treebank().isPreterminal(tree))
      return tree;
    if (tree.isList()) {
      SexpList treeList = tree.list();
      for (int i = 0; i < treeList.length(); i++) {
	Sexp currChild = treeList.get(i);
	if (!Language.treebank().isPreterminal(tree) &&
	    currChild.isList() && currChild.list().length() == 1)
	  treeList.remove(i--);
	else
	  removeChildlessNodes(currChild);
      }
    }
    return tree;
  }
}
