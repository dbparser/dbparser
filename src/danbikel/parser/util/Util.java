package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;

/**
 * Contains basic utility functions for <code>Sexp</code> objects that
 * represent parse trees.
 * @author Dan Bikel
 */
public class Util {

  private Util() {}

  /**
   * Returns a <code>SexpList</code> that contains all the leaves of the
   * specified parse tree.
   *
   * @param tree the tree from which to collect leaves (words)
   * @return a list of the words contained in the specified tree
   */
  public static Sexp collectLeaves(Sexp tree) {
    SexpList leaves = new SexpList();
    collectLeaves(tree, leaves, false);
    return leaves;
  }

  /**
   * Returns a <code>SexpList</code> that contains all the words of the
   * specified parse tree as well as their part of speech tags, where each
   * word is its own <code>SexpList</code> of the form <tt>(word (tag))</tt>.
   *
   * @param tree the tree from which to collect tagged words
   * @return a list of tagged words from the specified tree
   */
  public static Sexp collectTaggedWords(Sexp tree) {
    SexpList taggedWords = new SexpList();
    collectLeaves(tree, taggedWords, true);
    return taggedWords;
  }

  private static void collectLeaves(Sexp tree, SexpList leaves,
                                    boolean withTags) {
    if (Language.treebank().isPreterminal(tree)) {
      if (!Language.treebank().isNullElementPreterminal(tree)) {
        Word word = Language.treebank().makeWord(tree);
        Sexp leaf = null;
        if (withTags) {
          SexpList tagList = new SexpList(1).add(word.tag());
          leaf = new SexpList(2).add(word.word()).add(tagList);
        }
        else
          leaf = word.word();

        leaves.add(leaf);
      }
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 0; i < treeListLen; i++) {
        collectLeaves(treeList.get(i), leaves, withTags);
      }
    }
  }

  /**
   * Returns a string containing the pretty-printed version of the specified
   * parse tree.
   *
   * @param tree the tree to pretty-print
   * @return a string containing the pretty-printed version of the specified
   * parse tree.
   */
  public static String prettyPrint(Sexp tree) {
    StringBuffer sb = new StringBuffer();
    prettyPrint(tree, sb, 0);
    return sb.toString();
  }
  private static void prettyPrint(Sexp tree, StringBuffer sb, int level) {
    for (int i = 0; i < level; i++)
      sb.append("  ");
    if (Language.treebank().isPreterminal(tree)) {
      sb.append(tree.toString());
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      sb.append("(").append(treeList.symbolAt(0));
      boolean prevChildWasSmall = true;
      for (int i = 1; i < treeListLen; i++) {
        Sexp child = treeList.get(i);
        if (prevChildWasSmall &&
            (child.isSymbol() || Language.treebank().isPreterminal(child))) {
          sb.append(" ").append(child);
          prevChildWasSmall = true;
        }
        else {
          sb.append("\n");
          prettyPrint(treeList.get(i), sb, level + 1);
          prevChildWasSmall = false;
        }
      }
      sb.append(")");
    }
    else
      sb.append(tree);
  }
}