package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.Serializable;
import java.util.*;

/**
 * An item in a <code>CKYChart</code> for use when parsing via a probabilistic
 * version of the CKY algorithm.
 *
 * @see CKYChart
 */
public class CKYItem extends Item implements SexpConvertible {
  // data members

  /** The label of this chart item. */
  private Symbol label;

  /** The head word of this chart item. */
  private Word headWord;

  /** The subcat frame representing the unmet requirements on the left
      side of the head as of the production of this chart item. */
  private Subcat leftSubcat;

  /** The subcat frame representing the unmet requirements on the right
      side of the head as of the production of this chart item. */
  private Subcat rightSubcat;

  /** The item representing the head child of the tree node represented by this
      chart item, or <code>null</code> if this item represents a
      preterminal. */
  private CKYItem headChild;

  /** A list of <code>CKYItem</code> objects that are the children to the left
      of the head child, with the head-adjacent child being last. */
  private SLNode leftChildren;

  /** A list of <code>CKYItem</code> objects that are the children to the right
      of the head child, with the head-adjacent child being last. */
  private SLNode rightChildren;

  /** The previous modifiers generated on this item's side of its parent's
      head child; if this item represents the head child of its parent, then
      this list should be empty (or <code>null</code>). */
  private SexpList previousMods;

  /** The index of the first word of the span covered by this item. */
  private int start;

  /** The index of the last word of the span covered by this item. */
  private int end;

  /** The boolean indicating whether the span covered by this item
      contains a verb. */
  private boolean containsVerb;

  /** The boolean indicating whether this item has received its stop
      probabilities. */
  private boolean stop;

  // constructor

  /**
   * Constructs a CKY chart item with the specified data.
   *
   * @param label the nonterminal label at the root of the implicit subtree
   * represented by this chart item
   * @param headWord the head word of the lexicalized nonterminal at the root
   * of this chart item's subtree
   * @param leftSubcat the subcat frame to the left of the head child of
   * the implicit subtree of this chart item
   * @param rightSubcat the subcat frame to the left of the head child of
   * the implicit subtree of this chart item
   * @param headChild the chart item that represents the subtree of the
   * head child of this chart item's subtree
   * @param leftChildren the list of chart items that represent the
   * left-modifier subtrees of this chart item's subtree
   * @param rightChildren the list of chart items that represent the
   * right-modifier subtrees of this chart item's subtree
   * @param previousMods the list of previously-generated modifiers
   * of the head child of the parent of this chart item's subtree
   * @param start the start index of the span of words covered by this
   * chart item
   * @param end the end index of the span of words covered by this
   * chart item
   * @param containsVerb a boolean indicating whether this chart item's
   * subtree contains a verb
   * @param stop a boolean indicating whether stop probabilities have been
   * computed for this chart item
   * @param logProb the score for this chart item (inside probability *
   * outside probability)
   */
  public CKYItem(Symbol label,
                 Word headWord,
                 Subcat leftSubcat,
                 Subcat rightSubcat,
                 CKYItem headChild,
                 SLNode leftChildren,
                 SLNode rightChildren,
                 SexpList previousMods,
                 int start,
                 int end,
                 boolean containsVerb,
                 boolean stop,
                 double logProb) {
    super(logProb);
    this.label = label;
    this.headWord = headWord;
    this.leftSubcat = leftSubcat;
    this.rightSubcat = rightSubcat;
    this.headChild = headChild;
    this.leftChildren = leftChildren;
    this.rightChildren = rightChildren;
    this.previousMods = previousMods;
    this.start = start;
    this.end = end;
    this.containsVerb = containsVerb;
    this.stop = stop;
  }

  /** Returns the symbol that is the label of this chart item. */
  public Object label() { return label; }

  /**
   * Sets the label of this chart item.
   *
   * @param label a <code>Symbol</code> object that is to be the label of
   * this chart item
   * @throws ClassCastException if <code>label</code> is not an instance of
   * <code>Symbol</code>
   */
  public void setLabel(Object label) {
    this.label = (Symbol)label;
  }

  /** Returns <code>true</code> if this item represents a preterminal. */
  public boolean isPreterminal() { return headChild == null; }

  /**
   * Returns <code>true</code> if and only if the specified object is
   * also an instance of a <code>CKYItem</code> and all elements of
   * this <code>CKYItem</code> are equal to those of the specified
   * <code>CKYItem</code>, except their left and right children lists
   * and their log probability values.
   */
  public boolean equals(Object obj) {
    if (!(obj instanceof CKYItem))
      return false;
    CKYItem other = (CKYItem)obj;
    return (this.isPreterminal() == other.isPreterminal() &&
	    this.label == other.label &&
	    this.headWord.equals(other.headWord) &&
            (this.headChild == null ||
	     this.headChild.label == other.headChild.label) &&
	    this.previousMods.equals(other.previousMods) &&
            this.leftSubcat.equals(other.leftSubcat) &&
            this.rightSubcat.equals(other.rightSubcat) &&
	    this.containsVerb == other.containsVerb &&
            this.stop == other.stop);
  }

  /**
   * Computes the hash code based on all elements used by the
   * {@link #equals} method.
   */
  public int hashCode() {
    int code = label.hashCode();
    code = (code * 31) + headWord.hashCode();
    if (leftSubcat != null)
      code = (code * 31) + leftSubcat.hashCode();
    if (rightSubcat != null)
      code = (code * 31) + rightSubcat.hashCode();
    if (headChild != null)
      code = (code * 31) + headChild.label().hashCode();
    code = (code * 31) + previousMods.hashCode();
    code = (code << 1) | (stop ? 1 : 0);
    code = (code << 1) | (containsVerb ? 1 : 0);
    return code;
  }

  public Sexp toSexp() {
    if (isPreterminal()) {
      return headWord.toSexp();
    }
    else {
      int len = leftChildren.size() + rightChildren.size() + 2;
      SexpList list = new SexpList(len);
      // first, add label of this node
      list.add(label);
      // then, add left subtrees in order
      for (SLNode curr = leftChildren; curr != null; curr = curr.next())
        list.add(((CKYItem)curr.data()).toSexp());
      // next, add head child's subtree
      list.add(headChild.toSexp());
      // finally, add right children in reverse order
      LinkedList rcList = rightChildren.toList();
      ListIterator it = rcList.listIterator(rcList.size());
      while (it.hasPrevious()) {
        CKYItem item = (CKYItem)it.previous();
        list.add(item.toSexp());
      }
      return list;
    }
  }
}
