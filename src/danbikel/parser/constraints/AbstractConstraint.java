package danbikel.parser.constraints;

import danbikel.parser.Item;

/**
 * A base class that throws an
 * <code>UnsupportedOperationException</code> for every optional operation
 * of the <code>Constraint</code> interface.  Note that this class is not
 * actually <code>abstract</code> in spite of its name, as it implements
 * <i>all</i> the methods of the {@link Constraint} interface.
 */
public class AbstractConstraint implements Constraint {

  /**
   * Constructs a new instance of this base class, typically called
   * implicitly by concrete subclasses.
   */
  protected AbstractConstraint() {
  }
  /**
   * Throws an {@link UnsupportedOperationException}.
   */
  public boolean isLeaf() {
    throw new java.lang.UnsupportedOperationException();
  }
  /**
   * Throws an {@link UnsupportedOperationException}.
   */
  public Constraint getParent() {
    throw new java.lang.UnsupportedOperationException();
  }
  /**
   * Throws an {@link UnsupportedOperationException}.
   */
  public boolean isSatisfiedBy(Item item) {
    throw new java.lang.UnsupportedOperationException();
  }
  /**
   * Throws an {@link UnsupportedOperationException}.
   */
  public boolean hasBeenSatisfied() {
    throw new java.lang.UnsupportedOperationException();
  }
  /**
   * Throws an {@link UnsupportedOperationException}.
   */
  public boolean isLocallySatisfiedBy(Item item) {
    throw new java.lang.UnsupportedOperationException();
  }
  /**
   * Throws an {@link UnsupportedOperationException}.
   */
  public boolean isViolatedBy(Item item) {
    throw new java.lang.UnsupportedOperationException();
  }
  /**
   * Throws an {@link UnsupportedOperationException}.
   */
  public boolean isViolatedByChild(Item childItem) {
    throw new java.lang.UnsupportedOperationException();
  }
}