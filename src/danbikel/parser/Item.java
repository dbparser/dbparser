package danbikel.parser;

import danbikel.lisp.*;
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

  public int compareTo(Object o) {
    Item otherItem = (Item)o;
    return (logProb < otherItem.logProb ? -1 :
	    (logProb == otherItem.logProb ? 0 :
	     1));
  }
}
