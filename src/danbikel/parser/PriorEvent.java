package danbikel.parser;


import danbikel.lisp.*;
import java.io.Serializable;

/**
 * A class to represent the prior probabilities of lexicalized nonterminals.
 */
public class PriorEvent implements TrainerEvent, Cloneable {
  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method.
  private Word headWord;
  private Symbol label;

  /**
   * Constructs a new <code>PriorEvent</code> object, setting all its
   * data members to the specified values.
   *
   * @param headWord the head word
   * @param label the unlexicalized nonterminal label
   */
  public PriorEvent(Word headWord, Symbol label) {
    this.headWord = headWord;
    this.label = label;
  }

  // accessors
  public Word headWord() { return headWord; }
  public Symbol label() { return label; }
  /**
   * Returns the same symbol for all instances of this class, so that priors
   * may be computed via the same mechanism as conditional probabilities: if
   * the conditioning context is the same for all events counted, then the MLEs
   * for those conditional events are the same as would be the MLEs for the
   * prior probabilities of the predicted events.  That is, when computing MLEs
   * via counting, P(X | Y) = P_prior(X) if Y is always the same.
   */
  public Symbol history() { return Language.training().stopSym(); }
  public Symbol parent() { throw new UnsupportedOperationException(); }
  public Symbol head() { throw new UnsupportedOperationException(); }
  public Word modHeadWord() { throw new UnsupportedOperationException(); }


  // mutators
  void set(Word headWord, Symbol label) {
    this.headWord = headWord;
    this.label = label;
  }
  void setHeadWord(Word headWord) { this.headWord = headWord; }
  void setLabel(Symbol label) { this.label = label; }

  /**
   * Throws an <code>UnsupportedOperationException</code>, as this is not
   * a modifier event.
   *
   * @exception UnsupportedOperationException because this is not a modifier
   * event
   */
  public boolean side() { throw new UnsupportedOperationException(); }

  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (!(obj instanceof PriorEvent))
      return false;
    PriorEvent other = (PriorEvent)obj;
    boolean headWordsEqual = (headWord == null ? other.headWord == null :
			      headWord.equals(other.headWord));
    return headWordsEqual && label == other.label;
  }

  public String toString() {
    return "(" + headWord + " " + label + ")";
  }

  public int hashCode() {
    int code = 0;
    if (headWord != null)
      code = headWord.hashCode();
    code = (code << 2) ^ label.hashCode();
    return code;
  }

  /** Returns a deep copy of this object. */
  public Object clone() { return copy(); }
  /** Returns a deep copy of this object. */
  public TrainerEvent copy() {
    return new PriorEvent(headWord.copy(), label);
  }
}
