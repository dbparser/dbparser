package danbikel.parser.english;

import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import danbikel.lisp.*;
import danbikel.parser.Language;
import danbikel.parser.Nonterminal;

/**
 * Provides data and methods speciifc to the structures found in the English
 * Treebank (the
 * <a target="_blank"
 * href="http://www.cis.upenn.edu/~treebank/">Penn Treebank</a>) or any other
 * treebank that conforms to the Treebank II annotation guidelines for
 * <a target="_blank" href="ftp://ftp.cis.upenn.edu/pub/treebank/doc/tagguide.ps.gz">part-of-speech tagging</a>
 * and
 * <a target="_blank"
 * href="ftp://ftp.cis.upenn.edu/pub/treebank/doc/manual/">bracketing</a>.
 */
public class Treebank extends danbikel.parser.Treebank {

  // the characters that are delimiters of augmented nonterminal labels
  private final static String augmentationDelimStr = "-=|";

  // basic nodes in the English Treebank that will be transformed in a
  // preprocessing phase
  static final Symbol NP = Symbol.add("NP");
  static final Symbol S = Symbol.add("S");
  // transformed nodes
  static final Symbol baseNP = Symbol.add(NP + "B");
  static final Symbol subjectlessS = Symbol.add(S + "G");
  static final Symbol subjectAugmentation = Symbol.add("SBJ");

  // other basic nodes
  static final Symbol WHNP = Symbol.add("WHNP");
  static final Symbol CC = Symbol.add("CC");
  static final Symbol comma = Symbol.add(",");

  // null element
  static final Symbol nullElementPreterminal = Symbol.add("-NONE-");
  // possessive part of speech
  static final Symbol possessivePos = Symbol.add("POS");

  // exception nonterminals for defaultParseNonterminal
  static final Symbol[] nonterminalExceptionArr = {Symbol.add("-LRB-"),
						   Symbol.add("-RRB-")};

  private static Symbol[][] canonicalLabelMapData = {
    {baseNP, NP},
    {subjectlessS, S},
  };
  private static HashMap canonicalLabelMap =
    new HashMap(canonicalLabelMapData.length);
  static {
    for (int i = 0; i < canonicalLabelMapData.length; i++)
      canonicalLabelMap.put(canonicalLabelMapData[i][0],
			    canonicalLabelMapData[i][1]);
  }

  private static String[] puncToRaiseElements = {",", ":"};

  private static Set puncToRaise = new HashSet();
  static {
    for (int i = 0; i < puncToRaiseElements.length; i++)
      puncToRaise.add(Symbol.add(puncToRaiseElements[i]));
  }

  private static String[] verbTagStrings = {"VB", "VBD", "VBG", "VBN",
					    "VBP", "VBZ"};
  private static Set verbTags = new HashSet();
  static {
    for (int i = 0; i < verbTagStrings.length; i++)
      verbTags.add(Symbol.add(verbTagStrings[i]));
  }

  /**
   * Constructs an English <code>Treebank</code> object.
   */
  public Treebank() {
    super();
    nonterminalExceptionSet = nonterminalExceptionArr;
  }

  /**
   * Returns <code>true</code> if <code>tree</code> represents a preterminal
   * subtree (part-of-speech tag and word).  Specifically, this method
   * returns <code>true</code> if <code>tree</code> is an instance of
   * <code>SexpList</code>, has a length of 2 and has a first list element
   * of type <code>Symbol</code>.
   */
  public final boolean isPreterminal(Sexp tree) {
    return (tree.isList() &&
	    tree.list().length() == 2 &&
	    tree.list().get(0).isSymbol() &&
	    tree.list().get(1).isSymbol());
  }

  /**
   * Returns <code>true</code> is the specified nonterminal label represents a
   * sentence in the Penn Treebank, that is, if the canonical version of
   * <code>label</code> is equal to <code>&quot;S&quot;</code>.
   * @see danbikel.parser.english.Training#relabelSubjectlessSentences(Sexp)
   */
  public boolean isSentence(Symbol label) { return getCanonical(label) == S; }

  /**
   * Returns the symbol that
   * {@link danbikel.parser.english.Training#relabelSubjectlessSentences(Sexp)
   * relabelSubjectlessSentences}
   * will use for sentences that have no subjects.
   */
  public Symbol subjectlessSentenceLabel() { return subjectlessS; }

  public Symbol subjectAugmentation() { return subjectAugmentation; }

  /**
   * Returns <code>true</code> if the specified S-expression represents a
   * preterminal whose terminal element is the null element
   * (<code>&quot;-NONE-&quot;</code>) for the Penn Treebank.
   * @see danbikel.parser.english.Training#relabelSubjectlessSentences(Sexp)
   */
  public boolean isNullElementPreterminal(Sexp tree) {
    return (isPreterminal(tree) &&
	    tree.list().get(0).symbol() == nullElementPreterminal);
  }

  /**
   * Returns <code>true</code> if the specified S-expression is a preterminal
   * whose part of speech is <code>&quot;,&quot;</code> or
   * <code>&quot;.&quot;</code>.
   */
  public boolean isPuncToRaise(Sexp preterm) {
    return (isPreterminal(preterm) &&
	    puncToRaise.contains(preterm.list().first()));
  }

  public boolean isPunctuation(Symbol tag) {
    return puncToRaise.contains(stripAugmentation(tag));
  }

  /**
   * Returns <code>true</code> if the specified S-expression represents
   * a preterminal that is the possessive part of speech.  This method is
   * intended to be used by implementations of {@link
   * Training#addBaseNPs(Sexp)}.
   */
  public boolean isPossessivePreterminal(Sexp tree) {
    return (isPreterminal(tree) &&
	    tree.list().get(0).symbol() == possessivePos);
  }

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is an NP for for English Treebank.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public boolean isNP(Symbol label) {
    return getCanonical(label) == NP;
  }

  /**
   * Returns the symbol with which {@link Training#addBaseNPs(Sexp)} will
   * relabel base NPs.
   *
   * @see Training#addBaseNPs
   */
  public Symbol baseNPLabel() { return baseNP; }

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is a WHNP in the English Treebank.
   *
   * @see Training#addGapInformation(Sexp)
   */
  public boolean isWHNP(Symbol label) { return getCanonical(label) == WHNP; }

  /**
   * Returns the symbol that {@link Training#addBaseNPs(Sexp)} should
   * add as a parent if a base NP is not dominated by an NP.
   */
  public Symbol NPLabel() { return NP; }

  /**
   * Returns <code>true</code> if <code>label</code> is equal to the symbol
   * whose print name is <code>&quot;CC&quot;</code>.
   */
  public boolean isConjunction(Symbol label) {
    return getCanonical(label) == CC;
  }

  /**
   * Returns <code>true</code> if <code>preterminal</code> represents a
   * terminal with one of the following parts of speech: <tt>VB, VBD, VBG,
   * VBN, VBP</tt> or <tt>VBZ</tt>.  It is an error to call this method
   * with a <code>Sexp</code> object for which {@link #isPreterminal(Sexp)}
   * returns <code>false</code>.<br>
   *
   * @param preterminal the preterminal to test
   * @return <code>true</code> if <code>preterminal</code> is a verb
   */
  public boolean isVerb(Sexp preterminal) {
    return isVerbTag(preterminal.list().get(0).symbol());
  }

  public boolean isVerbTag(Symbol tag) {
    return verbTags.contains(tag);
  }

  public boolean isComma(Symbol word) {
    return word == comma;
  }

  /**
   * Returns a canonical mapping for the specified nonterminal label; if
   * <code>label</code> already is in canonical form, it is returned.
   * The canonical mapping refers to transformations performed on nonterminals
   * during the training process.  Before obtaining a label's canonical form,
   * it is also stripped of all Treebank augmentations, meaning that only
   * the characters before the first occurrence of '-', '=' or '|' are kept.
   *
   * @return a <code>Symbol</code> with the same print name as
   * <code>label</code>, except that all training transformations and Treebank
   * augmentations have been undone and stripped
   */
  public final Symbol getCanonical(Symbol label) {
    for (int i = 0; i < nonterminalExceptionSet.length; i++)
      if (label == nonterminalExceptionSet[i])
        return label;
    Symbol strippedLabel = stripAugmentation(label);
    Symbol mapEntry = (Symbol)canonicalLabelMap.get(strippedLabel);
    return ((mapEntry == null) ? strippedLabel : mapEntry);
  }

  public final Symbol getCanonical(Symbol label, boolean stripAugmentations) {
    if (stripAugmentations)
      return getCanonical(label);
    else {
      Symbol mapEntry = (Symbol)canonicalLabelMap.get(label);
      return (mapEntry == null ? label : mapEntry);
    }
  }

  /**
   * Calls {@link Treebank#defaultParseNonterminal(Symbol, Nonterminal)} with
   * the specified arguments.
   * @param label to the nonterminal label to parse
   * @param nonterminal the <code>Nonterminal</code> object to fill with
   * the components of <code>label</code>
   */
  public Nonterminal parseNonterminal(Symbol label, Nonterminal nonterminal) {
    defaultParseNonterminal(label, nonterminal);
    return nonterminal;
  }

  /**
   * Returns a string of the three characters that serve as augmentation
   * delimiters in the Penn Treebank: <code>&quot;-=|&quot;</code>.
   */
  public String augmentationDelimiters() { return augmentationDelimStr; }
}
