package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.io.Serializable;
import java.util.*;
import java.text.*;

/**
 * An item in a <code>CKYChart</code> for use when parsing via a probabilistic
 * version of the CKY algorithm.
 *
 * @see CKYChart
 */
public class CKYItem extends Item implements SexpConvertible {

  /**
   * The value of {@link Treebank#baseNPLabel}, cached for efficiency and
   * convenience.
   */
  protected final static Symbol baseNP = Language.treebank().baseNPLabel();
  /**
   * The value of the property {@link Settings#numPrevMods}, cached here
   * for efficiency and convenience.
   */
  protected final static int numPrevMods =
    Integer.parseInt(Settings.get(Settings.numPrevMods));
  /**
   * The value of the property {@link Settings#numPrevWords}, cached here
   * for efficiency and convenience.
   */
  protected final static int numPrevWords =
    Integer.parseInt(Settings.get(Settings.numPrevWords));

  public static class BaseNPAware extends CKYItem {
    public BaseNPAware() {
      super();
    }

    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (!(obj instanceof BaseNPAware))
        return false;
      BaseNPAware other = (BaseNPAware)obj;
      if (stop && other.stop) {
        return (this.isPreterminal() == other.isPreterminal() &&
                this.label == other.label &&
                this.headWord.equals(other.headWord));
      }
      else {
        return (this.isPreterminal() == other.isPreterminal() &&
                this.stop == other.stop &&
		this.leftVerb == other.leftVerb &&
                this.rightVerb == other.rightVerb &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
                (this.label == baseNP ? prevWordsEqual(other) : true) &&
                (this.headChild == null ||
		 this.headChild.label == other.headChild.label) &&
		this.leftPrevMods.equals(other.leftPrevMods) &&
                this.rightPrevMods.equals(other.rightPrevMods) &&
                this.leftSubcat.equals(other.leftSubcat) &&
                this.rightSubcat.equals(other.rightSubcat));
      }
    }

    public int hashCode() {
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();
      if (stop) {
        code = (code << 1) | (isPreterminal() ? 1 : 0);
        return code;
      }
      if (leftSubcat != null)
        code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
        code = (code << 2) ^ rightSubcat.hashCode();
      if (headChild != null)
        code = (code << 2) ^ headChild.label().hashCode();
      code = (code << 2) ^ leftPrevMods.hashCode();
      code = (code << 2) ^ rightPrevMods.hashCode();
      int booleanCode = 0;
      booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
      booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
      return code ^ booleanCode;
    }
  };

  /**
   * Overrides <code>equals</code> and <code>hashCode</code> methods
   * to take the first previous modifier into account only insofar as
   * its equality to the initial {@link Training#startSym} modifier.
   */
  public static class PrevModIsStart extends CKYItem {
    protected final static Symbol startSym = Language.training.startSym();

    public PrevModIsStart() {
      super();
    }
    /**
     * Returns <code>true</code> if and only if the specified object is
     * also an instance of a <code>CKYItem</code> and all elements of
     * this <code>CKYItem</code> are equal to those of the specified
     * <code>CKYItem</code>, except their left and right children lists
     * and their log probability values.
     */
    public boolean equals(Object obj) {
      if (this == obj)
	return true;
      if (!(obj instanceof PrevModIsStart))
	return false;
      PrevModIsStart other = (PrevModIsStart)obj;
      if (stop && other.stop) {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord));
      }
      else {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.stop == other.stop &&
		this.leftVerb == other.leftVerb &&
		this.rightVerb == other.rightVerb &&
		this.label == other.label &&
                this.headWord.equals(other.headWord) &&
		(this.headChild == null ||
		 this.headChild.label == other.headChild.label) &&
		this.leftPrevModIsStart() == other.leftPrevModIsStart() &&
		this.rightPrevModIsStart() == other.rightPrevModIsStart() &&
		this.leftSubcat.equals(other.leftSubcat) &&
		this.rightSubcat.equals(other.rightSubcat));
      }
    }

    protected boolean leftPrevModIsStart() {
      return leftPrevMods.get(0) == startSym;
    }
    protected boolean rightPrevModIsStart() {
      return rightPrevMods.get(0) == startSym;
    }

    /**
     * Computes the hash code based on all elements used by the
     * {@link #equals} method.
     */
    public int hashCode() {
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();
      if (stop) {
	code = (code << 1) | (isPreterminal() ? 1 : 0);
	return code;
      }
      if (leftSubcat != null)
	code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
	code = (code << 2) ^ rightSubcat.hashCode();
      if (headChild != null)
	code = (code << 2) ^ headChild.label().hashCode();
      int booleanCode = 0;
      booleanCode = (booleanCode << 1) | (leftPrevModIsStart() ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightPrevModIsStart() ? 1 : 0);
      booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
      booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
      return code ^ booleanCode;
    }
  };
  /**
   * Overrides <code>equals</code> and <code>hashCode</code> methods
   * to take the first previous modifier into account only insofar as
   * its equality to the initial {@link Training#startSym} modifier.
   */
  public static class MappedPrevModBaseNPAware extends CKYItem {
    public MappedPrevModBaseNPAware() {
      super();
    }
    /**
     * Returns <code>true</code> if and only if the specified object is
     * also an instance of a <code>CKYItem</code> and all elements of
     * this <code>CKYItem</code> are equal to those of the specified
     * <code>CKYItem</code>, except their left and right children lists
     * and their log probability values.
     */
    public boolean equals(Object obj) {
      if (this == obj)
	return true;
      if (!(obj instanceof MappedPrevModBaseNPAware))
	return false;
      MappedPrevModBaseNPAware other = (MappedPrevModBaseNPAware)obj;
      if (stop && other.stop) {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord));
      }
      else {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.stop == other.stop &&
		this.leftVerb == other.leftVerb &&
		this.rightVerb == other.rightVerb &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		(this.label == baseNP ? prevWordsEqual(other) : true) &&
		(this.headChild == null ||
		 this.headChild.label == other.headChild.label) &&
                this.prevModsEqual(other) &&
		this.leftSubcat.equals(other.leftSubcat) &&
		this.rightSubcat.equals(other.rightSubcat));
      }
    }

    protected boolean prevModsEqual(CKYItem other) {
      return (prevModsEqual(Constants.LEFT, other) &&
              prevModsEqual(Constants.RIGHT, other));
    }

    protected boolean prevModsEqual(boolean side, CKYItem other) {
      Symbol thisPrevMod = Collins.mapPrevMod(prevMods(side).symbolAt(0));
      Symbol otherPrevMod =
	Collins.mapPrevMod(other.prevMods(side).symbolAt(0));
      return thisPrevMod == otherPrevMod;
    }

    /**
     * Computes the hash code based on all elements used by the
     * {@link #equals} method.
     */
    public int hashCode() {
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();
      if (stop) {
	code = (code << 1) | (isPreterminal() ? 1 : 0);
	return code;
      }
      if (leftSubcat != null)
	code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
	code = (code << 2) ^ rightSubcat.hashCode();
      if (headChild != null)
	code = (code << 2) ^ headChild.label().hashCode();
      Symbol leftPrevMod = Collins.mapPrevMod(leftPrevMods.symbolAt(0));
      Symbol rightPrevMod = Collins.mapPrevMod(rightPrevMods.symbolAt(0));
      code = (code << 1) ^ leftPrevMod.hashCode();
      code = (code << 1) ^ rightPrevMod.hashCode();
      int booleanCode = 0;
      booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
      booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
      return code ^ booleanCode;
    }
  };

  // constants
  private final static int outputPrecision = 14;

  // static data members
  // number formatter for string (debugging) output
  private static NumberFormat doubleNF = NumberFormat.getInstance();
  static {
    doubleNF.setMinimumFractionDigits(outputPrecision);
    doubleNF.setMaximumFractionDigits(outputPrecision);
  }


  // data members

  /** The log of the probability of the tree represented by this item. */
  protected double logTreeProb;

  /**
   * The log of the probability of the lexicalized root nonterminal label of
   * the tree represented by this item.
   */
  protected double logPrior;

  /** The label of this chart item. */
  protected Symbol label;

  /** The head word of this chart item. */
  protected Word headWord;

  /** The subcat frame representing the unmet requirements on the left
      side of the head as of the production of this chart item. */
  protected Subcat leftSubcat;

  /** The subcat frame representing the unmet requirements on the right
      side of the head as of the production of this chart item. */
  protected Subcat rightSubcat;

  /** The item representing the head child of the tree node represented by this
      chart item, or <code>null</code> if this item represents a
      preterminal. */
  protected CKYItem headChild;

  /** A list of <code>CKYItem</code> objects that are the children to the left
      of the head child, with the head-adjacent child being last. */
  protected SLNode leftChildren;

  /** A list of <code>CKYItem</code> objects that are the children to the right
      of the head child, with the head-adjacent child being last. */
  protected SLNode rightChildren;

  /** The previous modifiers generated on the left of the head child. */
  protected SexpList leftPrevMods;

  /** The previous modifiers generated on the right of the head child. */
  protected SexpList rightPrevMods;

  /** The index of the first word of the span covered by this item. */
  protected int start;

  /** The index of the last word of the span covered by this item. */
  protected int end;

  /** The boolean indicating whether a verb intervenes between the head child
      and the currently-generated left-modifying child. */
  protected boolean leftVerb;

  /** The boolean indicating whether a verb intervenes between the head child
      and the currently-generated right-modifying child. */
  protected boolean rightVerb;

  /** The boolean indicating whether this item has received its stop
      probabilities. */
  protected boolean stop;

  /**
   * The boolean indicating whether this item has been eliminated from the
   * chart because another, equivalent item was added (meaning that this item
   * could not be immediately reclaimed, since the caller of
   * <code>Chart.add</code> may have a handle onto this item).
   */
  protected boolean garbage = false;

  // constructors

  /** Default constructor. Data members set to default values. */
  public CKYItem() {}

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
   * @param leftPrevMods the list of previously-generated modifiers
   * on the left of the head child of the parent of this chart item's subtree
   * @param rightPrevMods the list of previously-generated modifiers
   * on the right of the head child of the parent of this chart item's subtree
   * @param start the start index of the span of words covered by this
   * chart item
   * @param end the end index of the span of words covered by this
   * chart item
   * @param leftVerb a boolean indicating whether this chart item's head child
   * has a left-modifying subtree that contains a verb
   * @param rightVerb a boolean indicating whether this chart item's head child
   * has a right-modifying subtree that contains a verb
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
                 SexpList leftPrevMods,
                 SexpList rightPrevMods,
                 int start,
                 int end,
                 boolean leftVerb,
                 boolean rightVerb,
                 boolean stop,
                 double logTreeProb,
                 double logPrior,
                 double logProb) {
    super(logProb);
    this.logTreeProb = logTreeProb;
    this.logPrior = logPrior;
    this.label = label;
    this.headWord = headWord;
    this.leftSubcat = leftSubcat;
    this.rightSubcat = rightSubcat;
    this.headChild = headChild;
    this.leftChildren = leftChildren;
    this.rightChildren = rightChildren;
    this.leftPrevMods = leftPrevMods;
    this.rightPrevMods = rightPrevMods;
    this.start = start;
    this.end = end;
    this.leftVerb = leftVerb;
    this.rightVerb = rightVerb;
    this.stop = stop;
  }

  public void set(Symbol label,
                  Word headWord,
                  Subcat leftSubcat,
                  Subcat rightSubcat,
                  CKYItem headChild,
                  SLNode leftChildren,
                  SLNode rightChildren,
                  SexpList leftPrevMods,
                  SexpList rightPrevMods,
                  int start,
                  int end,
                  boolean leftVerb,
                  boolean rightVerb,
                  boolean stop,
                  double logTreeProb,
                  double logPrior,
                  double logProb) {
    this.logProb = logProb;
    this.logTreeProb = logTreeProb;
    this.logPrior = logPrior;
    this.label = label;
    this.headWord = headWord;
    this.leftSubcat = leftSubcat;
    this.rightSubcat = rightSubcat;
    this.headChild = headChild;
    this.leftChildren = leftChildren;
    this.rightChildren = rightChildren;
    this.leftPrevMods = leftPrevMods;
    this.rightPrevMods = rightPrevMods;
    this.start = start;
    this.end = end;
    this.leftVerb = leftVerb;
    this.rightVerb = rightVerb;
    this.stop = stop;
  }

  /** Returns the symbol that is the label of this chart item. */
  public Object label() { return label; }

  public Word headWord() { return headWord; }

  public Subcat leftSubcat() { return leftSubcat; }

  public Subcat rightSubcat() { return rightSubcat; }

  public CKYItem headChild() { return headChild; }

  public SLNode leftChildren() { return leftChildren; }

  public int numLeftChildren() {
    return leftChildren == null ? 0 : leftChildren.size();
  }

  public SLNode rightChildren() { return rightChildren; }

  public int numRightChildren() {
    return rightChildren == null ? 0 : rightChildren.size();
  }

  public SexpList leftPrevMods() { return leftPrevMods; }

  public SexpList rightPrevMods() { return rightPrevMods; }

  public int start() { return start; }

  public int end() { return end; }

  public boolean leftVerb() { return leftVerb; }

  public boolean rightVerb() { return rightVerb; }

  public boolean stop() { return stop; }

  public double logProb() { return logProb; }

  public double logTreeProb() { return logTreeProb; }

  public double logPrior() { return logPrior; }

  public Symbol headLabel() {
    if (isPreterminal())
      throw new UnsupportedOperationException();
    return headChild.label;
  }

  public boolean garbage() { return garbage; }

  // side-sensitive accessors
  public Subcat subcat(boolean side) {
    return side == Constants.LEFT ? leftSubcat : rightSubcat;
  }
  public SLNode children(boolean side) {
    return side == Constants.LEFT ? leftChildren : rightChildren;
  }
  public SexpList prevMods(boolean side) {
    return side == Constants.LEFT ? leftPrevMods : rightPrevMods;
  }
  public boolean verb(boolean side) {
    return side == Constants.LEFT ? leftVerb : rightVerb;
  }

  public boolean containsVerb() {
    return leftVerb || rightVerb || Language.treebank.isVerbTag(headWord.tag());
  }

  public int edgeIndex(boolean side) {
    return side == Constants.LEFT ? start : end;
  }

  // mutators

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

  public void setLeftSubcat(Subcat leftSubcat) {
    this.leftSubcat = leftSubcat;
  }

  public void setRightSubcat(Subcat rightSubcat) {
    this.rightSubcat = rightSubcat;
  }

  public void setLogTreeProb(double logTreeProb) {
    this.logTreeProb = logTreeProb;
  }

  public void setLogProb(double logProb) { this.logProb = logProb; }

  public void setLogPrior(double logPrior) { this.logPrior = logPrior; }

  // side-sensitive mutators
  public void setSubcat(boolean side, Subcat subcat) {
    if (side == Constants.LEFT)
      this.leftSubcat = subcat;
    else
      this.rightSubcat = subcat;
  }

  public void setChildren(boolean side, SLNode children) {
    if (side == Constants.LEFT)
      this.leftChildren = children;
    else
      this.rightChildren = children;
  }

  public void setPrevMods(boolean side, SexpList prevMods) {
    if (side == Constants.LEFT)
      this.leftPrevMods = prevMods;
    else
      this.rightPrevMods = prevMods;
  }

  public void setEdgeIndex(boolean side, int index) {
    if (side == Constants.LEFT)
      this.start = index;
    else
      this.end = index;
  }

  public void setVerb(boolean side, boolean verb) {
    if (side == Constants.LEFT)
      this.leftVerb = verb;
    else
      this.rightVerb = verb;
  }

  public void setSideInfo(boolean side,
                          Subcat subcat,
                          SLNode children,
                          SexpList prevMods,
                          int edgeIndex,
                          boolean verb) {
    setSubcat(side, subcat);
    setChildren(side, children);
    setPrevMods(side, prevMods);
    setEdgeIndex(side, edgeIndex);
    setVerb(side, verb);
  }

  public void setGarbage(boolean garbage) {
    this.garbage = garbage;
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
    if (this == obj)
      return true;
    if (!(obj instanceof CKYItem))
      return false;
    CKYItem other = (CKYItem)obj;
    if (stop && other.stop) {
      return (this.isPreterminal() == other.isPreterminal() &&
              this.label == other.label &&
              this.headWord.equals(other.headWord));
    }
    else {
      return (this.isPreterminal() == other.isPreterminal() &&
              this.stop == other.stop &&
  	      this.leftVerb == other.leftVerb &&
              this.rightVerb == other.rightVerb &&
   	      this.label == other.label &&
              this.headWord.equals(other.headWord) &&
              (this.headChild == null ||
  	       this.headChild.label == other.headChild.label) &&
  	      this.leftPrevMods.equals(other.leftPrevMods) &&
              this.rightPrevMods.equals(other.rightPrevMods) &&
              this.leftSubcat.equals(other.leftSubcat) &&
              this.rightSubcat.equals(other.rightSubcat));
    }
  }

  protected boolean prevWordsEqual(CKYItem other) {
    return prevWordsEqual(Constants.LEFT, other) &&
           prevWordsEqual(Constants.RIGHT, other);
  }

  protected boolean prevWordsEqual(boolean side, CKYItem other) {
    SLNode thisCurr = children(side);
    SLNode otherCurr = children(side);
    int counter = numPrevWords;
    while (counter > 0 && thisCurr != null && otherCurr != null) {
      CKYItem thisMod = (CKYItem)thisCurr.data();
      CKYItem otherMod = (CKYItem)otherCurr.data();
      if (!thisMod.headWord().equals(otherMod.headWord()))
        return false;
      thisCurr = thisCurr.next();
      otherCurr = otherCurr.next();
      counter--;
    }
    if (counter > 0 && !(thisCurr == null && otherCurr == null))
      return false;
    return true;
  }

  /**
   * Computes the hash code based on all elements used by the
   * {@link #equals} method.
   */
  public int hashCode() {
    int code = label.hashCode();
    code = (code << 2) ^ headWord.hashCode();
    if (stop) {
      code = (code << 1) | (isPreterminal() ? 1 : 0);
      return code;
    }
    if (leftSubcat != null)
      code = (code << 2) ^ leftSubcat.hashCode();
    if (rightSubcat != null)
      code = (code << 2) ^ rightSubcat.hashCode();
    if (headChild != null)
      code = (code << 2) ^ headChild.label().hashCode();
    code = (code << 2) ^ leftPrevMods.hashCode();
    code = (code << 2) ^ rightPrevMods.hashCode();
    int booleanCode = 0;
    booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
    booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
    booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
    return code ^ booleanCode;
  }

  public Sexp toSexp() {
    if (isPreterminal()) {
      return headWord.toSexp();
    }
    else {
      int len = numLeftChildren() + numRightChildren() + 2;
      SexpList list = new SexpList(len);
      // first, add label of this node
      list.add(label);
      // then, add left subtrees in order
      for (SLNode curr = leftChildren; curr != null; curr = curr.next())
        list.add(((CKYItem)curr.data()).toSexp());
      // next, add head child's subtree
      list.add(headChild.toSexp());
      // finally, add right children in reverse order
      if (rightChildren != null) {
        LinkedList rcList = rightChildren.toList();
        ListIterator it = rcList.listIterator(rcList.size());
        while (it.hasPrevious()) {
          CKYItem item = (CKYItem)it.previous();
          list.add(item.toSexp());
        }
      }
      return list;
    }
  }

  public String toString() {
    return toSexp().toString() + "\t\t; head=" + headWord +
      "; lc=" + leftSubcat.toSexp() + "; rc=" + rightSubcat.toSexp() +
      "; leftPrev=" + leftPrevMods + "; rightPrev=" + rightPrevMods +
      "; lv=" + shortBool(leftVerb) + "; rv=" + shortBool(rightVerb) +
      "; stop=" + shortBool(stop) +
      "\t; tree=" + doubleNF.format(logTreeProb) +
      "; prior=" + doubleNF.format(logPrior) +
      "; prob=" + doubleNF.format(logProb) +
      " (@" + System.identityHashCode(this) + ")";
  }

  private final static String shortBool(boolean bool) {
    return bool ? "t" : "f";
  }

  /**
   * Assigns data members of specified <code>CKYItem</code> to this item,
   * effectively performing a destructive shallow copy of the specified
   * item into this item.
   *
   * @param other the item whose data members are to be assigned to this
   * instance
   * @return this item
   */
  public CKYItem setDataFrom(CKYItem other) {
    this.label = other.label;
    this.headWord = other.headWord;
    this.leftSubcat = other.leftSubcat;
    this.rightSubcat = other.rightSubcat;
    this.headChild =  other.headChild;
    this.leftChildren = other.leftChildren;
    this.rightChildren = other.rightChildren;
    this.leftPrevMods = other.leftPrevMods;
    this.rightPrevMods = other.rightPrevMods;
    this.start = other.start;
    this.end = other.end;
    this.leftVerb = other.leftVerb;
    this.rightVerb = other.rightVerb;
    this.stop = other.stop;
    this.logTreeProb = other.logTreeProb;
    this.logProb = other.logProb;
    this.logPrior = other.logPrior;
    return this;
  }

  public CKYItem shallowCopy() {
    return new CKYItem(label, headWord,
                       leftSubcat, rightSubcat,
                       headChild, leftChildren, rightChildren,
                       (SexpList)leftPrevMods.deepCopy(),
                       (SexpList)rightPrevMods.deepCopy(),
                       start, end,
                       leftVerb, rightVerb, stop,
                       logTreeProb, logPrior, logProb);
  }
}
