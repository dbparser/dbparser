package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.*;

/**
 * A class to represent the modifier generation event implicit in the models
 * supported by this parsing package.  The class {@link Trainer} counts
 * such events, from which other events are derived.
 */
public class ModifierEvent implements TrainerEvent, Cloneable {
  private final static Symbol leftSym = Constants.leftSym;
  private final static Symbol rightSym = Constants.rightSym;
  private final static Symbol trueSym = Constants.trueSym;

  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method
  private Word modHeadWord;
  private Word headWord;
  private Symbol modifier;
  private SexpList previousMods;
  private WordList previousWords;
  private Symbol parent;
  private Symbol head;
  private Subcat subcat;
  private boolean verbIntervening;
  private boolean side;

  /**
   * Constructs a new object from the specified S-expression. The
   * <code>Sexp</code> must be an instance of a list with the following format:
   * <pre>
   * (modHeadWord headWord modifier previousMods parent head subcat
   *  verbIntervening side)
   * </pre>
   * where
   * <ul>
   * <li> <tt>modHeadWord</tt> is  a list of the form accepted by
   * {@link Word#Word(Sexp)}
   * <li> <tt>headWord</tt> is a list of the form accepted by
   * {@link Word#Word(Sexp)}
   * <li> <tt>modifier</tt> is the nonterminal label of the modifier
   * <li> <tt>previousMods</tt> is a list of previous modifying  nonterminal
   * labels
   * <li> <tt>parent</tt> is the parent nonterminal label
   * <li> <tt>head</tt> is the head child nonterminal label
   * <li> <tt>subcat</tt> is an ordered list of nonterminals representing
   * arguments that have yet to be generated
   * <li> <tt>verbIntervening</tt> is one of {{@link Constants#trueSym},
   * {@link Constants#falseSym}}
   * <li> <tt>side</tt> is one of {{@link Constants#leftSym},
   * {@link Constants#rightSym}}
   * </ul>
   */
  public ModifierEvent(Sexp sexp) {
    this(new Word(sexp.list().get(0)),
	 new Word(sexp.list().get(1)),
	 sexp.list().symbolAt(2),
	 SexpList.getCanonical(sexp.list().listAt(3)),
         WordListFactory.newList(sexp.list().listAt(4)),
	 sexp.list().symbolAt(5),
	 sexp.list().symbolAt(6),
	 sexp.list().listAt(7),
	 sexp.list().symbolAt(8) == trueSym,
	 (sexp.list().symbolAt(9) == leftSym ?
	  Constants.LEFT : Constants.RIGHT));
  }

  /**
   * Constructs a new <code>ModifierEvent</code> object, settings its
   * data members to the values specified.
   *
   * @param modHeadWord the head word of the modifying nonterminal of this
   * modifier event
   * @param headWord the head word of the head child being modified
   * @param modifier the nonterminal label of the modifier
   * @param previousMods a list of previous modifying nonterminal labels
   * @param parent the parent nonterminal label
   * @param head the head child nonterminal label
   * @param subcat an ordered list of arguments of the head that have yet to
   * be generated
   * @param verbIntervening a boolean representing whether or a not a verb
   * has been generated anywhere in the subtrees between the head child
   * and the current modifier
   * @param side a boolean that is equal to {@link Constants#LEFT} if this
   * modifier lies on the left side of the head child or equal to
   * {@link Constants#RIGHT} if this modifier lies on the right side
   */
  public ModifierEvent(Word modHeadWord,
		       Word headWord,
		       Symbol modifier,
		       SexpList previousMods,
                       WordList previousWords,
		       Symbol parent,
		       Symbol head,
		       SexpList subcat,
		       boolean verbIntervening,
		       boolean side) {
    this(modHeadWord, headWord, modifier, previousMods, previousWords, parent,
         head, Subcats.get(subcat), verbIntervening, side);
  }

  public ModifierEvent(Word modHeadWord,
		       Word headWord,
		       Symbol modifier,
		       SexpList previousMods,
                       WordList previousWords,
		       Symbol parent,
		       Symbol head,
		       Subcat subcat,
		       boolean verbIntervening,
		       boolean side) {
    set(modHeadWord, headWord, modifier, previousMods, previousWords, parent,
        head, subcat, verbIntervening, side);
  }

  // accessors
  /** Returns the head word of the modifier of this modifier event. */
  public Word modHeadWord() { return modHeadWord; }
  /** Returns the head word of the head child being modified. */
  public Word headWord() { return headWord; }
  /** Returns the nonterminal label of this modifier event. */
  public Symbol modifier() { return modifier; }
  /** Returns a list of modifiers that have already been generated. */
  public SexpList previousMods() { return previousMods; }
  /**
   * Returns a list of the head words of modifiers that have already been
   * generated.
   */
  public WordList previousWords() { return previousWords; }
  /** Returns the parent nonterminal label. */
  public Symbol parent() { return parent; }
  /** Returns the head child nonterminal label. */
  public Symbol head() { return head; }
  /**
   * Returns a list of arguments of the head child that have yet to be
   * generated.
   */
  public Subcat subcat() { return subcat; }
  /**
   * Returns whether a verb has been generated in any of the subtrees generated
   * between the current modifier and the head child.
   */
  public boolean verbIntervening() { return verbIntervening; }
  /**
   * Returns the value of {@link Constants#LEFT} if this modifier lies on the
   * left side of the head child, or the value of {@link Constants#RIGHT} if
   * this modifier lies on the right side.
   */
  public boolean side() { return side; }

  // package-access mutators
  void setModHeadWord(Word modHeadWord) {
    this.modHeadWord = modHeadWord;
  }
  void setHeadWord(Word headWord) {
    this.headWord = headWord;
  }
  void setModifier(Symbol modifier) {
    this.modifier = modifier;
  }
  void setPreviousMods(SexpList previousMods) {
    this.previousMods = previousMods;
  }
  void setPreviousWords(WordList previousWords) {
    this.previousWords = previousWords;
  }
  void setParent(Symbol parent) {
    this.parent = parent;
  }
  void setHead(Symbol head) {
    this.head = head;
  }
  void setSubcat(Subcat subcat) {
    this.subcat = subcat;
  }
  void setVerbIntervening(boolean verbIntervening) {
    this.verbIntervening = verbIntervening;
  }
  void setSide(boolean side) {
    this.side = side;
  }
  void set(Word modHeadWord,
           Word headWord,
           Symbol modifier,
           SexpList previousMods,
           WordList previousWords,
           Symbol parent,
           Symbol head,
           Subcat subcat,
           boolean verbIntervening,
           boolean side) {
    this.modHeadWord = modHeadWord;
    this.headWord = headWord;
    this.modifier = modifier;
    this.previousMods = SexpList.getCanonical(previousMods);
    this.previousWords = previousWords;
    this.parent = parent;
    this.head = head;
    this.subcat = subcat;
    this.verbIntervening = verbIntervening;
    this.side = side;
  }

  /**
   * Returns <code>true</code> if the specified object is an instance of
   * a <code>ModifierEvent</code> object containing data members which are all
   * pairwise-equal with the data members of this <code>ModifierEvent</code>
   * object, according to each data member's <code>equals(Object)</code> method.
   */
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (!(o instanceof ModifierEvent))
      return false;
    ModifierEvent other = (ModifierEvent)o;
    boolean modHeadWordsEqual =
      (modHeadWord == null ? other.modHeadWord == null :
       modHeadWord.equals(other.modHeadWord));
    boolean headWordsEqual =
      (headWord == null ? other.headWord == null :
       headWord.equals(other.headWord));
    return (modHeadWordsEqual &&
	    headWordsEqual &&
	    modifier == other.modifier &&
	    previousMods.equals(other.previousMods) &&
            previousWords.equals(other.previousWords) &&
	    parent == other.parent &&
	    head == other.head &&
	    subcat.equals(other.subcat) &&
	    verbIntervening == other.verbIntervening &&
	    side == other.side);
  }

  /**
   * Returns an S-expression of the form accepted by
   * {@link ModifierEvent#ModifierEvent(Sexp)}.
   */
  public String toString() {
    return "(" + modHeadWord + " " + headWord +
      " " + modifier + " " + previousMods + " " + previousWords.toSexp() +
      " " + parent + " " + head + " " +
      subcat.toSexp() + " " + verbIntervening + " " +
      (side == Constants.LEFT ? leftSym : rightSym) + ")";
  }

  /**
   * Returns the hash code of this object, calculated from the hash codes
   * of all its data members.
   */
  public int hashCode() {
    int code = 0;
    if (modHeadWord != null)
      code = modHeadWord.hashCode();
    if (headWord != null)
      code = (code << 2) ^ headWord.hashCode();
    code = (code << 2) ^ modifier.hashCode();
    code = (code << 2) ^ previousMods.hashCode();
    code = (code << 2) ^ previousWords.hashCode();
    code = (code << 2) ^ parent.hashCode();
    code = (code << 2) ^ head.hashCode();
    code = (code << 2) ^ subcat.hashCode();
    int booleansCode = (((verbIntervening ? 1 : 0) << 1) |
			(side ? 1 : 0));
    code = (code << 2) | booleansCode;
    return code;
  }

  /** Returns a deep copy of this object. */
  public Object clone() {
    return copy();
  }
  /** Returns a deep copy of this object. */
  public TrainerEvent copy() {
    return new ModifierEvent(modHeadWord.copy(),
			     headWord.copy(),
			     modifier,
			     new SexpList(previousMods),
                             previousWords.copy(),
			     parent,
			     head,
			     (Subcat)subcat.copy(),
			     verbIntervening,
			     side);
  }

  private void readObject(ObjectInputStream in)
  throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    System.err.println("reading a ModifierEvent!");
  }
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.defaultWriteObject();
    System.err.println("outputting a ModifierEvent!");
  }
}
