package danbikel.parser;

import danbikel.lisp.*;
import danbikel.util.Text;
import java.util.*;

/**
 * A <code>Treebank</code> implementation provides data and methods specific
 * to the structures found in a particular Treebank.
 * <p>
 * A language package must provide an implementation of this interface.
 */
public interface Treebank {
  /**
   * Returns whether <code>tree</code> represents a preterminal subtree in the
   * parse trees for this language's Treebank.  Typically, preterminals are
   * part-of-speech tags.
   */
  public boolean isPreterminal(Sexp tree);

  /**
   * Gets the component of the preterminal tree that corresponds to the
   * part of speech tag.
   *
   * @param preterminal a tree that is assumed to be a preterminal
   * @return the symbol in <code>preterminal</code> that is a part of speech
   */
  public Symbol getTag(Sexp preterminal);

  /**
   * Constructs a <code>Word</code> object from the specified preterminal
   * subtree.
   *
   * @param preterminal a tree that is assumed to be a preterminal
   * @return the symbol in <code>preterminal</code> that is a part of speech
   */
  public Word makeWord(Sexp preterminal);

  /**
   * Converts a {@link Word} object into a preterminal subtree.
   *
   * @param word the word object from which to create a preterminal subtree
   * @return a preterminal subtree constructed from <code>word</code>
   */
  public Sexp constructPreterminal(Word word);

  /**
   * Returns a canonical mapping for the specified nonterminal label; if
   * <code>label</code> already is in canonical form, it is returned.
   * This method is intended to be used by implementations of
   * {@link HeadFinder#findHead(Sexp)}.
   *
   * @see HeadFinder#findHead(Sexp)
   */
  public Symbol getCanonical(Symbol label);

  public Symbol getCanonical(Symbol label, boolean stripAugmentations);

  /**
   * Returns <code>true</code> is the specified nonterminal label represents a
   * sentence in the current language's Treebank.  This method is intended to
   * be used by implementations of {@link
   * Training#relabelSubjectlessSentences(Sexp)}.
   */
  public boolean isSentence(Symbol label);

  /**
   * Returns the canonical label for a sentence, for de-transforming sentences
   * that were transformed via {@link
   * Training#relabelSubjectlessSentences(Sexp)}.
   */
  public Symbol sentenceLabel();

  /**
   * Returns the symbol with which {@link
   * Training#relabelSubjectlessSentences(Sexp)}
   * will relabel sentences when they have no subjects.
   */
  public Symbol subjectlessSentenceLabel();

  /**
   * Returns the symbol that is used to augment nonterminals to indicate matrix
   * subjects in the current language's Treebank.
   *
   * @see Training#relabelSubjectlessSentences(Sexp)
   */
  public Symbol subjectAugmentation();

  /**
   * Returns <code>true</code> if the specified S-expression represents
   * a preterminal whose terminal element is the null element for the current
   * language's Treebank.  This method is intended to be used by implementations
   * of {@link Training#relabelSubjectlessSentences(Sexp)}.
   *
   * @see Training#relabelSubjectlessSentences(Sexp)
   */
  public boolean isNullElementPreterminal(Sexp tree);

  /**
   * Returns the index of a trace for the specified null element preterminal.
   * If <code>preterm</code> is not a null element preterminal (that is, a
   * preterminal for which {@link #isNullElementPreterminal(Sexp)} returns
   * <code>false</code>), the semantics of this method are undefined.
   *
   * @param preterm the null element preterminal whose trace index is to be
   * returned
   * @param nonterminal the object used as the second argument to
   * {@link #parseNonterminal(Symbol,Nonterminal)}
   * @return the index of the trace of the terminal contained in
   * <code>preterm</code>, or -1 if the null element does not have an index
   */
  public int getTraceIndex(Sexp preterm, Nonterminal nonterminal);

  /**
   * Returns <code>true</code> if the specified S-expression represents
   * a preterminal and a part-of-speech tag that indicates punctuation
   * to be raised when running {@link Training#raisePunctuation(Sexp)}.  If
   * punctuation raising is not desirable for a particular language
   * package, this method may be implemented simply to return
   * <code>false</code>.
   *
   * @param preterm the preterminal to test
   * @see Training#raisePunctuation(Sexp)
   */
  public boolean isPuncToRaise(Sexp preterm);

  /**
   * Returns <code>true</code> if the specified part of speech tag is one
   * for which {@link #isPuncToRaise(Sexp)} would return <code>true</code>.
   *
   * @param tag the part of speech to test
   * @see #isPuncToRaise(Sexp)
   */
  public boolean isPunctuation(Symbol tag);

  /**
   * Returns <code>true</code> if the specified S-expression represents
   * a preterminal that is the possessive part of speech.  This method is
   * intended to be used by implementations of {@link
   * Training#addBaseNPs(Sexp)}.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public boolean isPossessivePreterminal(Sexp tree);

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is an NP for the current language's Treebank.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public boolean isNP(Symbol label);

  /**
   * Returns the symbol with which {@link Training#addBaseNPs(Sexp)} will
   * relabel core NPs.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public Symbol baseNPLabel();

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is an NP that undergoes WH-movement in a particular Treebank.  This method
   * is used by {@link Training#addGapInformation(Sexp)}.  If a particular
   * language package does not require gap information, then this method may be
   * implemented simply to return <code>false</code>.
   *
   * @see Training#addGapInformation(Sexp)
   */
  public boolean isWHNP(Symbol label);

  /**
   * Returns the symbol that {@link Training#addBaseNPs(Sexp)} should
   * add as a parent if a base NP is not dominated by an NP.
   *
   * @see Training#addBaseNPs(Sexp)
   */
  public Symbol NPLabel();

  /**
   * Returns <code>true</code> if the canonical version of the specified label
   * is a conjunction tag or nonterminal in a particular Treebank.
   */
  public boolean isConjunction(Symbol label);

  /**
   * Returns <code>true</code> if the specified preterminal is that of a verb.
   * This method is used by {@link HeadTreeNode} to determine if a particular
   * subtree contains a verb, which is in turn used by {@link Trainer} to
   * calculate the distance metric, which depends on whether a verb occurs
   * in the subtrees of the previous modifiers.  It is the responsibility
   * of the caller to insure that <code>preterminal</code> is a
   * <code>Sexp</code> object for which {@link #isPreterminal(Sexp)} returns
   * <code>true</code>.
   *
   * @see HeadTreeNode
   * @see Trainer
   */
  public boolean isVerb(Sexp preterminal);

  /**
   * Returns <code>true</code> if the specified symbol is the part of speech
   * tag of a verb.  This method should return true for exactly the same
   * parts of speech for which {@link #isVerb(Sexp)} returns <code>true</code>,
   * and is used to calculate the distance metric while decoding.
   *
   * @see CKYItem#containsVerb()
   * @see Decoder
   */
  public boolean isVerbTag(Symbol tag);

  /**
   * Returns <code>true</code> if the specified word is a comma.  This method
   * is used by the <code>Decoder</code> class when performing the comma
   * constraint on chart items.
   *
   * @param word the word to test
   * @see Settings#decoderUseCommaConstraint
   */
  public boolean isComma(Symbol word);

  /**
   * Returns <code>true</code> if the specified word is a left
   * parenthesis.  This method is used by the <code>Decoder</code>
   * class when performing the comma constraint on chart items.
   *
   * @param word the word to test
   * @see Settings#decoderUseCommaConstraint
   */
  public boolean isLeftParen(Symbol word);

  /**
   * Returns <code>true</code> if the specified word is a right
   * parenthesis.  This method is used by the <code>Decoder</code>
   * class when performing the comma constraint on chart items.
   *
   * @param word the word to test
   * @see Settings#decoderUseCommaConstraint
   */
  public boolean isRightParen(Symbol word);

  /**
   * Returns a string whose characters are the set of delimiters for
   * complex nonterminal labels.
   *
   * @see #stripAugmentation(Symbol)
   * @see #defaultParseNonterminal(Symbol,Nonterminal)
   */
  public String augmentationDelimiters();

  /**
   * Returns the first character of the string returned by
   * {@link #augmentationDelimiters}, which will be considered the
   * &quot;canonical&quot; augmentation delimiter when adding
   * new augmentations, such as the argument augmentations added by
   * implementations of {@link Training#identifyArguments(Sexp)}.
   */
  public char canonicalAugDelimiter();

  /**
   * Returns the <code>Symbol</code> created by stripping off all
   * augmentations, that is all characters after and including the first
   * character that appears in the string returned by
   * {@link #augmentationDelimiters}.
   *
   * @param label the potentially-complex nonterminal label to be stripped
   * @return a version of <code>label</code> with all augmentations removed
   */
  public Symbol stripAugmentation(Symbol label);

  /**
   * Returns <code>label</code>, but stripped of any index augmentation.  This
   * method assumes that the index will always be the final augmentation in a
   * complex nonterminal label.<br><b>N.B.</b>: This method will create a new
   * <code>Nonterminal</code> object, to be filled in by {@link
   * #stripIndex(Symbol,Nonterminal)}.
   *
   * @param label the nonterminal to be stripped of any possible index
   * @return a <code>Symbol</code> that is identical to <code>label</code>,
   * except that all characters after and including the final delimiter
   * are removed if the final augmentation is composed entirely of digits
   */
  public Symbol stripIndex(Symbol label);

  /**
   * Identical to {@link #stripIndex(Symbol)}, except that instead of creating
   * a new <code>Nonterminal</code> object for use by {@link
   * #parseNonterminal(Symbol,Nonterminal)}, this method simply passes the
   * specified <code>nonterminal</code> object.  In a sequential run, this
   * method provides maximum efficiency, as only one <code>Nonterminal</code>
   * object need be created at the beginning of the run.
   */
  public Symbol stripIndex(Symbol label, Nonterminal nonterminal);

  /**
   * Returns a symbol identical to the specified <code>label</code>, except
   * all augmentations other than the index will be removed.  If
   * <code>label</code> had no index to begin with, then this method
   * is functionally identical to {@link #stripAugmentation(Symbol)}.
   * @param label the nonterminal label to strip of non-index augmentations
   */
  public Symbol stripAllButIndex(Symbol label);

  /**
   * Identical to {@link #stripAllButIndex(Symbol)}, except that instead of
   * creating a new <code>Nonterminal</code> object for use by
   * {@link #parseNonterminal(Symbol,Nonterminal)}, this method
   * uses the specified <code>nonterminal</code> object.  In a sequential
   * run, this method provides maximum efficiency, as only one
   * <code>Nonterminal</code> object need be created at the beginning
   * of the run.
   */
  public Symbol stripAllButIndex(Symbol label, Nonterminal nonterminal);

  /**
   * Returns a <code>Nonterminal</code> object to represent all the
   * components of a complex nonterminal annotation: the base label, any
   * augmentations and any index.  If there are no augmentations, the
   * <code>augmentations</code> field of the returned object will contain
   * a list with zero elements; if there is no index, the
   * value of index will be -1.  A final requirement of the contract of this
   * method is to represent all the delimiters in the list of augmentations;
   * this requirement is met, for example, by the helper method {@link
   * #defaultParseNonterminal(Symbol,Nonterminal)}.<br>
   * <b>Efficiency note</b>: This method creates and returns a new
   * <code>Nonterminal</code> object with every invocation.
   *
   * @param label a (possibly complex) nonterminal label from a Treebank
   * @return a <code>Nonterminal</code> object representing any and
   * all components of the specified complex nonterminal
   * @see Nonterminal
   */
  public Nonterminal parseNonterminal(Symbol label);

  /**
   * Identical to {@link #parseNonterminal(Symbol)}, except that instead of
   * returning a newly-created <code>Nonterminal</code> object, this
   * method merely modifies the specified <code>Nonterminal</code> object.
   * This method may be used for efficiency: in a particular, sequential
   * training run, only one <code>Nonterminal</code> need be created,
   * repeatedly passed in to this method for modification.
   *
   * @param label a (possibly complex) nonterminal label from a Treebank
   * @param nonterminal the representation of any and all components present
   * in <code>label</code>
   */
  public Nonterminal parseNonterminal(Symbol label,
				      Nonterminal nonterminal);


  /**
   * Fills in the specified <code>Nonterminal</code> object to represent
   * all the components of a complex nonterminal annotation: the base label,
   * any augmentations and any index.  If there are no augmentations, the
   * <code>augmentations</code> field of the returned object will contain a
   * list with no elements; if there is no index, the value of index will be
   * -1.  Augmentation delimiters are the characters in the string returned by
   * {@link #augmentationDelimiters}.<br><b>N.B.</b>: This method assumes that
   * the index, if one exists for the specified nonterminal, will always be the
   * final augmentation in the label.<br>This method is intended to be used by
   * implementations of {@link #parseNonterminal(Symbol,Nonterminal)}.
   *
   * @param label a (possibly complex) nonterminal label from a Treebank
   * @see Nonterminal
   */
  public void defaultParseNonterminal(Symbol label, Nonterminal nonterminal);

  /**
   * Provides an efficient, thread-safe method for testing whether the
    * specified nonterminal contains the specified augmentation (without
   * parsing the nonterminal).
   * <p>
   * <b>N.B.</b>: This method assumes that the augmentation is preceded
   * by the canonical augmentation delimiter.  To search for an augmentation
   * preceded by <i>any</i> of the possible augmentaion delimiters (as defined
   * by {@link #augmentationDelimiters}), use
   * <pre>
   * parseNonterminal(nonterminal).augmentations.contains(augmentation)
   * </pre>
   */
  public boolean containsAugmentation(Symbol nonterminal,
				      Symbol augmentation);

  /**
   * Adds the specified augmentation to the end of the (possibly empty)
   * augmentation list of the specified <code>Nonterminal</code> object.
   * This method takes care to add the canonical augmentation delimiter
   * before adding the augmentation itself, and also takes care to add
   * these two elements before a final delimiter between the main augmentations
   * and the index, if one exists.
   *
   * @param nonterminal the nonterminal to which to add an augmentation
   * @param augmentation the augmentation to add to <code>nonterminal</code>'s
   * augmentation list
   */
  public void addAugmentation(Nonterminal nonterminal,
			      Symbol augmentation);

  /**
   * Removes the specified augmentation from the augmentation list of the
   * specified <code>Nonterminal</code> object, and the previous augmentation
   * delimiter.  If the specified augmentation is <i>not</i> preceded by
   * an augmentation delimiter, meaning it is the base label itself, then it
   * is not removed.
   *
   * @param nonterminal the nonterminal from which to remove an augmentation
   * @param augmentation the augmentation to remove from
   * <code>nonterminal</code>
   * @return <code>true</code> if <code>augmentation</code> and
   * a preceding augmentation delimiter was removed from
   * <code>nonterminal</code>'s augmentation list, or <code>false</code>
   * otherwise
   */
  public boolean removeAugmentation(Nonterminal nonterminal,
				    Symbol augmentation);

  public Sexp removeAugmentation(Sexp sexp,
				 Nonterminal nonterminal,
				 Symbol augmentation);

  public boolean isAugDelim(Sexp sexp);
}
