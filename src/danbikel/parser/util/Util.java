package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;

/**
 * Contains asic utility functions for <code>Sexp</code> objects that
 * represent parse trees.
 * @author Dan Bikel
 * @version 1.0
 */

public class Util {

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
}