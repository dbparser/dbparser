package danbikel.parser.constraints;

import danbikel.parser.Item;

/**
 * An abstract base class that throws an
 * <code>UnsupportedOperationException</code> for every optional operation
 * of the <code>Constraint</code> interface.
 */
public class AbstractConstraint implements Constraint {

  protected AbstractConstraint() {
  }
  public boolean isLeaf() {
    throw new java.lang.UnsupportedOperationException();
  }
  public Constraint getParent() {
    throw new java.lang.UnsupportedOperationException();
  }
  public boolean isSatisfiedBy(Item item) {
    throw new java.lang.UnsupportedOperationException();
  }
  public boolean hasBeenSatisfied() {
    throw new java.lang.UnsupportedOperationException();
  }
  public boolean isLocallySatisfiedBy(Item item) {
    throw new java.lang.UnsupportedOperationException();
  }
  public boolean isViolatedBy(Item item) {
    throw new java.lang.UnsupportedOperationException();
  }
  public boolean isViolatedByChild(Item childItem) {
    throw new java.lang.UnsupportedOperationException();
  }
}