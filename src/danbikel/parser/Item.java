package danbikel.parser;

import danbikel.lisp.*;
import danbikel.parser.constraints.*;
import java.io.Serializable;

/**
 * Skeletal class to represent items in a parsing chart.  Items implement the
 * comparable interface, so as to be sorted by their probability.
 *
 * @see Chart
 */
public abstract class Item implements Serializable, Comparable {
  // data member

  /** The log-probability of this chart item. */
  protected double logProb;

  // constructor

  /**
   * Constructs this item to have an initial log-probability of
   * <code>Constants.logOfZero</code>.  This constructor will be called,
   * often implicitly, by the constructor of a subclass.
   *
   * @see Constants#logOfZero
   */
  protected Item() {
    logProb = Constants.logOfZero;
  }

  protected Item(double logProb) {
    this.logProb = logProb;
  }

  /** Returns the label of this chart item. */
  public abstract Object label();

  /** Sets the label of this chart item. */
  public abstract void setLabel(Object label);

  /** Gets the log probability of this chart item. */
  public double logProb() { return logProb; }

  /** Sets the log probability of this chart item. */
  public void setLogProb(double logProb) { this.logProb = logProb; }

  /**
   * Returns whether this item has been eliminated from the chart because
   * another, equivalent item was added (meaning that this item could not
   * be immediately reclaimed, since the caller of
   * <code>Chart.add</code> may have a handle onto this item).
   *
   * @see Chart#add(int,int,Item)
   */
  public abstract boolean garbage();

  /**
   * Returns the constraint associated with this chart item, or
   * <code>null</code> if this item has no associated constraint.
   */
  public abstract Constraint getConstraint();

  /**
   * Sets the constraint for this item.
   * @param constraint the constraint to be associated with this item.
   */
  public abstract void setConstraint(Constraint constraint);

  /**
   * Sets the value of this item's garbage status.
   *
   * @see #garbage()
   * @see Chart#add(int,int,Item)
   */
  public abstract void setGarbage(boolean garbage);

  public int compareTo(Object o) {
    Item otherItem = (Item)o;
    return (logProb < otherItem.logProb ? -1 :
	    (logProb == otherItem.logProb ? 0 :
	     1));
  }
}
