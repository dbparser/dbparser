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

  protected final static boolean outputLexLabels =
  Boolean.valueOf(Settings.get(Settings.decoderOutputHeadLexicalizedLabels)).booleanValue();

  protected final static boolean baseNPsCannotContainVerbs =
  Boolean.valueOf(Settings.get(Settings.baseNPsCannotContainVerbs)).booleanValue();

  /**
   * The value of {@link Treebank#baseNPLabel}, cached for efficiency and
   * convenience.
   */
  protected final static Symbol baseNP = Language.treebank().baseNPLabel();

  /**
   * The value of {@link Training#topSym}, cached for efficiency and
   * convenience.
   */
  protected final static Symbol topSym = Language.training().topSym();

  /**
   * The value of {@link Training#stopWord()}, cached here for efficiency
   * and convenience.
   */
  protected final static Word stopWord = Language.training().stopWord();

  protected final static byte containsVerbUndefined = 0;
  protected final static byte containsVerbTrue = 1;
  protected final static byte containsVerbFalse = -1;

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

    public Symbol headLabel() {
      if (isPreterminal())
	return null;
      return (headChild.label == baseNP ?
	      headChild.headChild.label : headChild.label);
    }

    public boolean equals(Object obj) {
      if (this == obj)
	return true;
      if (!(obj instanceof BaseNPAware))
	return false;
      BaseNPAware other = (BaseNPAware)obj;
      if (this.label != topSym && stop && other.stop) {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		this.containsVerb() == other.containsVerb());
      }
      else if (this.label == baseNP) {
	return (this.stop == other.stop &&
		this.label == other.label &&
		//this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.leftPrevMods.equals(other.leftPrevMods) &&
		this.rightPrevMods.equals(other.rightPrevMods) &&
		this.prevWordsEqual(other));
      }
      else {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.stop == other.stop &&
		this.leftVerb == other.leftVerb &&
		this.rightVerb == other.rightVerb &&
		this.containsVerb() == other.containsVerb() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.leftPrevMods.equals(other.leftPrevMods) &&
		this.rightPrevMods.equals(other.rightPrevMods) &&
		this.leftSubcat.equals(other.leftSubcat) &&
		this.rightSubcat.equals(other.rightSubcat));
      }
    }

    public int hashCode() {
      // all three types of chart items (stopped, baseNP and others)
      // depend on label and head word
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();

      // special case for stopped items
      if (label != topSym && stop) {
	code = (code << 1) | (isPreterminal() ? 1 : 0);
	return code;
      }

      // special case for baseNP items
      if (this.label == baseNP) {
	Symbol headLabel = headLabel();
	if (headLabel != null)
	  code = (code << 2) ^ headLabel.hashCode();
	code = (code << 2) ^ leftPrevMods.hashCode();
	code = (code << 2) ^ rightPrevMods.hashCode();
	return code;
      }

      // finish computation of hash code for all other items
      if (leftSubcat != null)
	code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
	code = (code << 2) ^ rightSubcat.hashCode();
      Symbol headLabel = headLabel();
      if (headLabel != null)
	code = (code << 2) ^ headLabel.hashCode();
      code = (code << 2) ^ leftPrevMods.hashCode();
      code = (code << 2) ^ rightPrevMods.hashCode();
      int booleanCode = 0;
      //booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
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
      if (this.label != topSym && stop && other.stop) {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		this.containsVerb() == other.containsVerb());
      }
      else {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.stop == other.stop &&
		this.label == other.label &&
		this.leftVerb == other.leftVerb &&
		this.rightVerb == other.rightVerb &&
		this.containsVerb() == other.containsVerb() &&
		this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
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
      // for both types of items (stopped and un-stopped), the equals method
      // relies on items' labels and head words
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();

      // special case for stopped items
      if (label != topSym && stop) {
	code = (code << 1) | (isPreterminal() ? 1 : 0);
	return code;
      }

      // finish the hash code computation for un-stopped items
      if (leftSubcat != null)
	code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
	code = (code << 2) ^ rightSubcat.hashCode();
      if (headLabel() != null)
	code = (code << 2) ^ headLabel().hashCode();
      int booleanCode = 0;
      booleanCode = (booleanCode << 1) | (leftPrevModIsStart() ? 1 : 0);
      booleanCode = (booleanCode << 1) | (rightPrevModIsStart() ? 1 : 0);
      //booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
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

    /*
    public Symbol headLabel() {
      if (isPreterminal())
	return null;
      return (headChild.label == baseNP ?
	      headChild.headChild.label : headChild.label);
    }
    */

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
      if (this.label != topSym && stop && other.stop) {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.label == other.label &&
		this.headWord.equals(other.headWord) &&
		this.containsVerb() == other.containsVerb());
      }
      else if (this.label == baseNP) {
	return (this.stop == other.stop &&
		this.label == other.label &&
		//this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.leftPrevMods.equals(other.leftPrevMods) &&
		this.rightPrevMods.equals(other.rightPrevMods) &&
		this.prevWordsEqual(other));
      }
      else {
	return (this.isPreterminal() == other.isPreterminal() &&
		this.stop == other.stop &&
		this.label == other.label &&
		this.leftVerb == other.leftVerb &&
		this.rightVerb == other.rightVerb &&
		this.containsVerb() == other.containsVerb() &&
		this.headWord.equals(other.headWord) &&
		this.headLabel() == other.headLabel() &&
		this.mappedPrevModsEqual(other) &&
		this.leftSubcat.equals(other.leftSubcat) &&
		this.rightSubcat.equals(other.rightSubcat));
      }
    }

    protected boolean mappedPrevModsEqual(CKYItem other) {
      return
	((Collins.mapPrevMod(this.leftPrevMods.symbolAt(0)) ==
	  Collins.mapPrevMod(other.leftPrevMods.symbolAt(0))) &&

	 (Collins.mapPrevMod(this.rightPrevMods.symbolAt(0)) ==
	  Collins.mapPrevMod(other.rightPrevMods.symbolAt(0))));
    }

    /**
     * Computes the hash code based on all elements used by the
     * {@link #equals} method.
     */
    public int hashCode() {
      // all three types of chart items (stopped, baseNP and others)
      // depend on label and head word
      int code = label.hashCode();
      code = (code << 2) ^ headWord.hashCode();

      // special case for stopped items
      if (label != topSym && stop) {
	code = (code << 1) | (isPreterminal() ? 1 : 0);
	return code;
      }

      // special case for baseNP items
      if (this.label == baseNP) {
	Symbol headLabel = headLabel();
	if (headLabel != null)
	  code = (code << 2) ^ headLabel.hashCode();
	code = (code << 2) ^ leftPrevMods.hashCode();
	code = (code << 2) ^ rightPrevMods.hashCode();
	return code;
      }

      // finish computation of hash code for all other items
      if (leftSubcat != null)
	code = (code << 2) ^ leftSubcat.hashCode();
      if (rightSubcat != null)
	code = (code << 2) ^ rightSubcat.hashCode();
      Symbol headLabel = headLabel();
      if (headLabel != null)
	code = (code << 2) ^ headLabel.hashCode();
      Symbol mappedLeftPrevMod = Collins.mapPrevMod(leftPrevMods.symbolAt(0));
      Symbol mappedRightPrevMod = Collins.mapPrevMod(rightPrevMods.symbolAt(0));
      code = (code << 2) ^ mappedLeftPrevMod.hashCode();
      code = (code << 2) ^ mappedRightPrevMod.hashCode();
      int booleanCode = 0;
      //booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
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

  /** The constraint associated with this chart item. */
  protected Constraint constraint;

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

  protected byte containsVerb = containsVerbUndefined;

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
    containsVerb = containsVerbUndefined;
    garbage = false;
  }

  public Constraint getConstraint() { return constraint; }
  public void setConstraint(Constraint constraint) {
    this.constraint = constraint;
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
      return null;
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

  /*
  public boolean containsVerb() {
    return leftVerb || rightVerb || Language.treebank.isVerbTag(headWord.tag());
  }
  */

  public boolean containsVerb() {
    if (containsVerb == containsVerbUndefined)
      containsVerb =
	containsVerbRecursive() ? containsVerbTrue : containsVerbFalse;
    return containsVerb == containsVerbTrue;
  }

  protected boolean containsVerbRecursive() {
    if (baseNPsCannotContainVerbs && this.label == baseNP)
      return false;
    else if (leftVerb || rightVerb)
      return true;
    // it is crucial to check the head child BEFORE checking the head word
    // (which is, of course, inherited from the head child), since
    // the containsVerb predicate can be "blocked" by NPB's
    else if (headChild != null)
      return headChild.containsVerb();
    else
      return Language.treebank.isVerbTag(headWord.tag());
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
    containsVerb = containsVerbUndefined;
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
    if (this.label != topSym && stop && other.stop) {
      return (this.isPreterminal() == other.isPreterminal() &&
	      this.label == other.label &&
	      this.headWord.equals(other.headWord) &&
	      this.containsVerb() == other.containsVerb());
    }
    else {
      return (this.isPreterminal() == other.isPreterminal() &&
	      this.stop == other.stop &&
	      this.label == other.label &&
	      this.leftVerb == other.leftVerb &&
	      this.rightVerb == other.rightVerb &&
	      this.containsVerb() == other.containsVerb() &&
	      this.headWord.equals(other.headWord) &&
	      this.headLabel() == other.headLabel() &&
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

  /**
   * Returns whether the head words of modifier children on the specified
   * side of this item are equal to those on the specified side of the
   * specified other item.
   * <br>
   * <b>Implementation note</b>: This complicated method would not be
   * necessary if we stored appropriate WordList objects within chart items
   * (where "appropriate" means "created by the decoder using the Shifter
   * to skip items that need to be skipped").
   *
   * @param side the side on which to compare head words of modifier children
   * @param other the other chart item with which to compare modifier
   * head words
   * @return whether the previously-generated head words of modifier items
   * of this chart item are equal to those of the specified other chart item
   */
  protected boolean prevWordsEqual(boolean side, CKYItem other) {
    int counter = numPrevWords;

    SLNode thisCurr = children(side);
    SLNode otherCurr = other.children(side);

    // pretend that next word to be generated is stop word
    Word thisCurrModHead = stopWord;
    Word otherCurrModHead = stopWord;

    while (counter > 0 && thisCurr != null && otherCurr != null) {

      CKYItem thisMod = (CKYItem)thisCurr.data();
      CKYItem otherMod = (CKYItem)otherCurr.data();

      Word thisPrevModHead = thisMod.headWord();
      Word otherPrevModHead = otherMod.headWord();

      // we only look at this chart item (and not "other") for skipping
      if (!Shifter.skip(this, thisPrevModHead, thisCurrModHead)) {
	// here's where we actually compare head words
	if (thisPrevModHead.equals(otherPrevModHead)) {
	  counter--;
	}
	else {
	  return false;
	}
      }

      // since we're going back in time, curr becomes prev before next iteration
      thisCurrModHead = thisPrevModHead;
      otherCurrModHead = otherPrevModHead;

      thisCurr = thisCurr.next();
      otherCurr = otherCurr.next();
    }

    if (counter > 0) {
      // if we haven't finished checking numPrevWords, then end of at least one
      // of child lists was reached, SO...
      // if it is NOT the case that ends of *both* children lists were reached,
      // return false (since lists were not of equal length)
      if (!(thisCurr == null && otherCurr == null)) {
	return false;
      }
      // otherwise, simply return whether head child's head words are equal
      else {
	return this.headWord.equals(other.headWord);
      }
    }

    // it must be that counter == 0, so we've checked numPrevWords words
    return true;
  }

  /**
   * Computes the hash code based on all elements used by the
   * {@link #equals} method.
   */
  public int hashCode() {
    int code = label.hashCode();
    code = (code << 2) ^ headWord.hashCode();
    if (label != topSym && stop) {
      code = (code << 1) | (isPreterminal() ? 1 : 0);
      return code;
    }
    if (leftSubcat != null)
      code = (code << 2) ^ leftSubcat.hashCode();
    if (rightSubcat != null)
      code = (code << 2) ^ rightSubcat.hashCode();
    Symbol headLabel = headLabel();
    if (headLabel != null)
      code = (code << 2) ^ headLabel.hashCode();
    code = (code << 2) ^ leftPrevMods.hashCode();
    code = (code << 2) ^ rightPrevMods.hashCode();
    int booleanCode = 0;
    //booleanCode = (booleanCode << 1) | (stop ? 1 : 0);
    booleanCode = (booleanCode << 1) | (leftVerb ? 1 : 0);
    booleanCode = (booleanCode << 1) | (rightVerb ? 1 : 0);
    return code ^ booleanCode;
  }

  public Sexp toSexp() {
    return toSexpInternal(false);
  }

  private Sexp toSexpInternal(boolean isHeadChild) {
    if (isPreterminal()) {
      Sexp preterm = Language.treebank.constructPreterminal(headWord);
      if (outputLexLabels) {
	Symbol pretermLabel = preterm.list().symbolAt(0);
	preterm.list().set(0, getLabel(pretermLabel, isHeadChild));
      }
      return preterm;
    }
    else {
      int len = numLeftChildren() + numRightChildren() + 2;
      SexpList list = new SexpList(len);
      // first, add label of this node
      list.add(getLabel(label, isHeadChild));
      // then, add left subtrees in order
      for (SLNode curr = leftChildren; curr != null; curr = curr.next())
	list.add(((CKYItem)curr.data()).toSexpInternal(false));
      // next, add head child's subtree
      list.add(headChild.toSexpInternal(true));
      // finally, add right children in reverse order
      if (rightChildren != null) {
	LinkedList rcList = rightChildren.toList();
	ListIterator it = rcList.listIterator(rcList.size());
	while (it.hasPrevious()) {
	  CKYItem item = (CKYItem)it.previous();
	  list.add(item.toSexpInternal(false));
	}
      }
      return list;
    }
  }

  /**
   * Helper method used by {@link #toSexpInternal()}, to provide a layer of
   * abstraction so that the label can include, e.g., head information.
   *
   * @return the (possibly head-lexicalized) root label of the derivation of
   * this item
   */
  private Symbol getLabel(Symbol label, boolean isHeadChild) {
    if (outputLexLabels) {
      Nonterminal nt = Language.treebank.parseNonterminal(label);
      String newLabel =
	nt.base +
	"[" + Boolean.toString(isHeadChild) +
	"/" + headWord.word() + "/" + headWord.tag() + "]";
      nt.base =	Symbol.add(newLabel);
      return nt.toSymbol();
    }
    else
      return label;
  }

  public String toString() {
    return toSexp().toString() + "\t\t; head=" + headWord +
      "; lc=" + leftSubcat.toSexp() + "; rc=" + rightSubcat.toSexp() +
      "; leftPrev=" + leftPrevMods + "; rightPrev=" + rightPrevMods +
      "; lv=" + shortBool(leftVerb) + "; rv=" + shortBool(rightVerb) +
      "; hasVerb=" + shortContainsVerb(containsVerb) +
      "; stop=" + shortBool(stop) +
      "\t; tree=" + doubleNF.format(logTreeProb) +
      "; prior=" + doubleNF.format(logPrior) +
      "; prob=" + doubleNF.format(logProb) +
      " (@" + System.identityHashCode(this) + ")";
  }

  protected final static String shortBool(boolean bool) {
    return bool ? "t" : "f";
  }

  protected final static String shortContainsVerb(byte containsVerbValue) {
    return (containsVerbValue == containsVerbUndefined ? "undef" :
	    (containsVerbValue == containsVerbTrue ? "t" : "f"));
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
    this.constraint = other.constraint;
    containsVerb = containsVerbUndefined;
    garbage = false;
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
