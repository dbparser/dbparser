package danbikel.parser.constraints;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

/**
 * An implementation of a constraint to sit in a tree structure of constraints
 * that represent a subgraph (certain brackets) of a tree, constraining a
 * decoder to pursue only theories that contain the brackets of the
 * constraint set of these objects.
 */
public class PartialTreeConstraint implements Constraint, SexpConvertible {
  protected PartialTreeConstraint parent;
  protected List children;
  protected Symbol label;
  protected Nonterminal nt = new Nonterminal();
  protected Nonterminal otherNT = new Nonterminal();
  protected int start;
  protected int end;
  protected boolean satisfied;
  protected boolean fullySatisfied;

  public PartialTreeConstraint(Sexp tree) {
    this(null, tree, new IntCounter(0));
  }

  protected PartialTreeConstraint() {}

  protected PartialTreeConstraint(PartialTreeConstraint parent,
				Sexp tree, IntCounter currWordIdx) {
    if (Language.treebank().isPreterminal(tree)) {
      this.parent = parent;
      children = Collections.EMPTY_LIST;
      Word word = Language.treebank().makeWord(tree);
      label = word.tag();
      Language.treebank().parseNonterminal(label, nt);
      start = end = currWordIdx.get();
      currWordIdx.increment();
    }
    else {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      //label = Language.treebank().getCanonical(treeList.symbolAt(0));
      label = treeList.symbolAt(0);
      Language.treebank().parseNonterminal(label, nt);
      start = currWordIdx.get();
      this.parent = parent;
      children = new ArrayList(treeListLen - 1);
      for (int i = 1; i < treeListLen; i++) {
	Sexp child = treeList.get(i);
	children.add(new PartialTreeConstraint(this, child, currWordIdx));
      }
      end = currWordIdx.get() - 1;
    }
  }

  public boolean isLeaf() { return children.size() == 0; }

  public boolean isViolatedByChild(Item childItem) {
    return !spanOK(childItem);
  }

  public Constraint getParent() { return fullySatisfied ? parent : this; }


  protected List getChildren() { return children; }
  public Symbol label() { return label; }
  public int start() { return start; }
  public int end() { return end; }

  public boolean isViolatedBy(Item item) {
    throw new UnsupportedOperationException();
  }

  protected boolean isSatisfiedByPreterminal(CKYItem item) {
    //if (isLocallySatisfiedBy(item) && spanMatches(item)) {
    if (true) {
      satisfied = true;
      fullySatisfied = true;
      return true;
    }
    else
      return false;
  }

  /**
   * Returns <code>true</code> if this constraint is satisfied by its local
   * information.  Internally, a constraint is said to be <i>fully satisfied</i>
   * if the specified item has a matching span <i>and</i> a matching label,
   * as determined by the {@link #spanMatches(Item)} and
   * {@link #labelMatches(Item)} methods, respectively.
   *
   * @param item the item to test for satisfaction by this constraint
   * @return whether this constraint is satisfied the specified item
   *
   * @see #isLocallySatisfiedBy(Item)
   * @see #spanMatches(Item)
   * @see #labelMatches(Item)
   */
  public boolean isSatisfiedBy(Item item) {
    CKYItem ckyItem = (CKYItem)item;

    // normally, preterminal items should be assigned constraints from the
    // list of leaves, and thus this case should normally not be considered;
    // but just in case a brain-dead programmer creates a decoder that is
    // inefficient in this way, here's the code to deal with it
    if (ckyItem.isPreterminal()) {
      return isSatisfiedByPreterminal(ckyItem);
    }

    if (!isLocallySatisfiedBy(item))
      return false;

    satisfied = true;

    if (spanMatches(item) && labelMatches(item))
      fullySatisfied = true;

    return true;
  }

  public boolean hasBeenSatisfied() { return satisfied; }

  public boolean isLocallySatisfiedBy(Item item) {
    return spanOK(item);
  }

  /**
   * Returns whether the span of the specified item crosses the span.
   * of this constraint
   * @param item the item whose span is to be tested
   * @return whether the span of the specified item crosses the span
   */
  protected boolean spanOK(Item item) {
    CKYItem ckyItem = (CKYItem)item;
    return ckyItem.start() >= this.start && ckyItem.end() <= this.end();
  }

  protected boolean spanMatches(Item item) {
    CKYItem ckyItem = (CKYItem)item;
    return ckyItem.start() == start && ckyItem.end() == end;
  }

  /**
   * Returns whether this constraint's label subsumes the label of the
   * specified item.
   *
   * @param item the item whose label is to be tested
   * @return whether this constraint's label subsumes the label of the
   * specified item.
   *
   * @see Nonterminal#subsumes(Nonterminal)
   */
  protected boolean labelMatches(Item item) {
    Language.treebank().parseNonterminal((Symbol)item.label(), otherNT);
    return this.nt.subsumes(otherNT);
  }

  public Sexp toSexp() {
    SexpList retVal = new SexpList(children.size() + 1);

    Symbol thisNode = Symbol.add(label.toString() + "-" + start + "-" + end);

    retVal.add(thisNode);
    for (int i = 0; i < children.size(); i++)
      retVal.add(((SexpConvertible)children.get(i)).toSexp());

    return retVal;
  }

  public String toString() {
    return "label=" + label + ", span=(" + start + "," + end +
	   "), parentLabel=" +
	   (parent == null ? "null" : parent.label.toString());
  }
}
