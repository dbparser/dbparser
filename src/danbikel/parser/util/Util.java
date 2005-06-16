package danbikel.parser.util;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

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

  public static ArrayList collectWordObjects(Sexp tree) {
    return collectWordObjects(tree, new ArrayList());
  }

  private static ArrayList collectWordObjects(Sexp tree, ArrayList words) {
    if (Language.treebank().isPreterminal(tree)) {
      if (!Language.treebank().isNullElementPreterminal(tree)) {
	Word word = Language.treebank().makeWord(tree);
	words.add(word);
      }
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      for (int i = 0; i < treeListLen; i++) {
	collectWordObjects(treeList.get(i), words);
      }
    }
    return words;
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
   * Adds the nonterminals in the specified tree to the specified set.
   *
   * @param counts the counts table to which to add the nonterminals present in
   * the specified tree
   * @param tree the tree from which to collect nonterminals
   * @param includeTags indicates whether to treat part of speech tags
   * as nonterminals
   * @return the specified counts table, modified to contain the counts of the
   * nonterminals present in the specified tree
   */
  public static CountsTable collectNonterminals(CountsTable counts, Sexp tree,
					boolean includeTags) {
    return collectNonterminals(counts, tree, includeTags, false);
  }

  /**
   * Adds the part of speech tags in the specified tree to the specified set.
   *
   * @param counts the counts table to which to add the tags present in the
   * specified tree
   * @param tree the tree from which to collect part of speech tags
   * @return the specified counts table, modified to contain the counts of the
   * part of speech tags present in the specified tree
   */
  public static CountsTable collectTags(CountsTable counts, Sexp tree) {
    return collectNonterminals(counts, tree, true, true);
  }

  private static CountsTable collectNonterminals(CountsTable counts, Sexp tree,
                                                 boolean includeTags,
                                                 boolean onlyTags) {
    if (Language.treebank().isPreterminal(tree)) {
      if (includeTags) {
	Word word = Language.treebank().makeWord(tree);
	counts.add(word.tag());
      }
    }
    else if (tree.isList()) {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();
      if (!onlyTags)
	counts.add(treeList.get(0));
      for (int i = 0; i < treeListLen; i++) {
	collectNonterminals(counts, treeList.get(i), includeTags, onlyTags);
      }
    }
    return counts;
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

  /**
   * Adds <code>value</code> to the set that is the vale of <code>key</code>
   * in <code>map</code>; creates this set if a mapping doesn't already
   * exist for <code>key</code>.
   *
   * @param map the map to be updated
   * @param key the key in <code>map</code> whose value set is to be updated
   * @param value the value to be added to <code>key</code>'s value set
   */
  public final static void addToValueSet(Map map,
					 Object key,
					 Object value) {
    Set valueSet = (Set)map.get(key);
    if (valueSet == null) {
      valueSet = new HashSet();
      map.put(key, valueSet);
    }
    valueSet.add(value);
  }
}