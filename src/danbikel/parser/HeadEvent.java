package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * A class to represent the head generation event implicit in the models
 * supported by this parsing package.  The class {@link Trainer} counts
 * such events, from which other events are derived.
 */
public class HeadEvent implements TrainerEvent, Cloneable {
  // data members
  // N.B.: IF ANY MORE DATA MEMBERS ARE ADDED, make sure to update
  // the copy method
  private Word headWord;
  private Symbol parent;
  private Symbol head;
  private Subcat leftSubcat;
  private Subcat rightSubcat;

  /**
   * Contructs a new object from the specified S-expression.  The
   * <code>Sexp</code> must be an instance of a list with the
   * following format:
   * <pre> (headWord parent head leftSubcat rightSubcat) </pre>
   * where
   * <ul>
   * <li> <tt>headWord</tt> is an S-expression that is compatible with the
   * {@link Word#Word(Sexp) Sexp word constructor}
   * <li> <tt>parent</tt> is a parent nonterminal label
   * <li> <tt>head</tt> is a head child nonterminal label
   * <li> <tt>leftSubcat</tt> is a list of arguments (nonterminal labels) and
   * possibly other elements to be generated on the left side of the head child
   * <li> <tt>rightSubcat</tt> is a list of arguments (nonterminal labels) and
   * possibly other elements to be generated on the right side of the head child
   * </ul>
   *
   * @param sexp a list containing all the information necessary to
   * construct this <code>HeadEvent</code> object
   */
  public HeadEvent(Sexp sexp) {
    this(new Word(sexp.list().get(0)),
	 sexp.list().symbolAt(1),
	 sexp.list().symbolAt(2),
	 sexp.list().listAt(3),
	 sexp.list().listAt(4));
  }

  /**
   * Constructs a new <code>HeadEvent</code> object, setting all its
   * data members to the specified values.
   *
   * @param headWord the head word
   * @param parent the parent nonterminal label
   * @param head the head nonterminal label
   * @param leftSubcat the left subcategorization frame
   * @param rightSubcat the right subcategorization frame
   */
  public HeadEvent(Word headWord, Symbol parent, Symbol head,
		   SexpList leftSubcat, SexpList rightSubcat) {
    this.headWord = headWord;
    this.parent = parent;
    this.head = head;
    this.leftSubcat = Subcats.get(leftSubcat);
    this.rightSubcat = Subcats.get(rightSubcat);
  }

  public HeadEvent(Word headWord, Symbol parent, Symbol head,
		   Subcat leftSubcat, Subcat rightSubcat) {
    this.headWord = headWord;
    this.parent = parent;
    this.head = head;
    this.leftSubcat = leftSubcat;
    this.rightSubcat = rightSubcat;
  }
  

  // accessors
  /** Returns the head word of this head event. */
  public Word headWord() { return headWord; }
  /** Returns the parent nonterminal label of this head event. */
  public Symbol parent() { return parent; }
  /** Returns the head nonterminal label of this head event. */
  public Symbol head() { return head; }
  /** Returns the left subcategorization frame of this head event. */
  public Subcat leftSubcat() { return leftSubcat; }
  /** Returns the right subcategorization frame of this head event. */
  public Subcat rightSubcat() { return rightSubcat; }

  // accessors to comply with interface TrainerEvent
  /**
   * Returns <code>null</code>, as head events do not deal with modifier words.
   */
  public Word modHeadWord() { return null; }

  /**
   * Throws an <code>UnsupportedOperationException</code>, as this is not
   * a modifier event.
   *
   * @exception UnsupportedOperationException because this is not a modifier
   * event
   */
  public boolean side() { throw new UnsupportedOperationException(); }

  /**
   * Returns <code>true</code> if the specified object is an instance of
   * a <code>HeadEvent</code> object containing data members which are all
   * pairwise-equal with the data members of this <code>HeadEvent</code>
   * object, according to each data member's <code>equals(Object)</code> method.
   */
  public boolean equals(Object o) {
    if (!(o instanceof HeadEvent))
      return false;
    HeadEvent other = (HeadEvent)o;
    boolean headWordsEqual = (headWord == null ? other.headWord == null :
			      headWord.equals(other.headWord));
    return (headWordsEqual &&
	    parent == other.parent &&
	    head == other.head &&
	    leftSubcat.equals(other.leftSubcat) &&
	    rightSubcat.equals(other.rightSubcat));
  }

  /**
   * Returns an S-expression of the form accepted by
   * {@link HeadEvent#HeadEvent(Sexp)}.
   */
  public String toString() {
    return
      "(" + headWord + " " + parent + " " + head + " " + leftSubcat.toSexp() +
      " " + rightSubcat.toSexp() + ")";
  }

  /**
   * Returns the hash code of this object, calculated from the hash codes
   * of all its data members.
   */
  public int hashCode() {
    int code = 0;
    if (headWord != null)
      code = headWord.hashCode();
    code = (code << 2) ^ parent.hashCode();
    code = (code << 2) ^ head.hashCode();
    code = (code << 2) ^ leftSubcat.hashCode();
    code = (code << 2) ^ rightSubcat.hashCode();
    return code;
  }

  /** Returns a deep copy of this object. */
  public Object clone() { return copy(); }
  /** Returns a deep copy of this object. */
  public TrainerEvent copy() {
    return new HeadEvent(headWord.copy(),
			 parent,
			 head,
			 (Subcat)leftSubcat.copy(),
			 (Subcat)rightSubcat.copy());
  }
}
