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
   */
  public static Sexp collectLeaves(Sexp tree) {
    SexpList leaves = new SexpList();
    collectLeaves(tree, leaves);
    return leaves;
  }
  private static void collectLeaves(Sexp tree, SexpList leaves) {
    if (Language.treebank().isPreterminal(tree)) {
      if (!Language.treebank().isNullElementPreterminal(tree)) {
        Word word = Language.treebank().makeWord(tree);
        leaves.add(word.word());
      }
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 0; i < treeListLen; i++) {
        collectLeaves(treeList.get(i), leaves);
      }
    }
  }

  /**
   * Returns a string containing the pretty-printed version of the specified
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