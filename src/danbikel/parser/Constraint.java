package danbikel.parser;

/**
 * Specifies methods to check a chart item's satisfaction or violation of a
 * parsing constraint.  A simple type of constraint, for example, might only
 * allow a chart item that is consistent with a particular bracketing.  More
 * specific constraints might impose a specific derivation when decoding, for
 * example, so as to guarantee that the tree that gets built is homologous to
 * a particular labeled, bracketed tree.
 *
 * @author Dan Bikel
 * @see ConstraintSet
 * @see Item
 */
public interface Constraint {
  /**
   * Returns true if this constraint is part of a tree structure of constraints
   * and is a leaf (optional operation).
   *
   * @return true if this constraint is part of a tree structure of constraints
   * and is a leaf.
   *
   * @see ConstraintSet#hasTreeStructure()
   */
  public boolean isLeaf();

  /**
   * Gets the parent constraint of this constraint (optional operation).
   *
   * @return the parent of this constraint
   *
   * @see ConstraintSet#hasTreeStructure()
   */
  public Constraint getParent();

  /**
   * Returns whether the specified item satisfies this constraint.  If this
   * constraint is part of a tree structure of constraints, then this method
   * should typically only return <code>true</code> if and only if the entire
   * subtree represented by the specified item satisfies the homologous
   * subtree of this constraint and its descendants.
   *
   * @param item the item to test
   * @return whether the specified item satisfies this constraint.
   */
  public boolean isSatisfiedBy(Item item);

  /**
   * Returns whether the specified item satisfies the local information
   * of the constraint node, regardless of its place in a tree structure
   * of constraints (optional operation).  When bottom-up parsing and
   * building a parse theory from some child item <code>c</code>, this
   * method may be used to determine if some proposed parent item
   * <code>p</code> locally satisfies the information contained in
   * <code>c.getConstraint().getParent()</code>.  That is, if
   * <pre>c.getConstraint().getParent().isLocallySatisfiedBy(p)</pre>
   * returns <code>true</code>, then theories building on the proposed
   * parent item <code>p</code> should be pursued.  Local constraint
   * information may include, for example, a nonterminal label or span
   * information.
   *
   * @param item
   * @return
   */
  public boolean isLocallySatisfiedBy(Item item);

  /**
   * Returns whether the specified item violates this constraint.
   *
   * @param item the item to test
   * @return whether the specified item violates this constraint.
   */
  public boolean isViolatedBy(Item item);

  /**
   * Returns whether the specified child item satisfies this constraint
   * (optional operation).  For implementations that do not implement a
   * recursive notion of constraint satisfaction, this method should simply
   * return <code>true</code> regardless of the value of the argument.
   *
   * @param childItem the child item to test
   * @return whether the specified child item satisfies this constraint.
   *
   * @see ConstraintSet#hasTreeStructure()
   */
  public boolean isSatisfiedByChild(Item childItem);
}