package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;

/**
 * Represents a set of constraints that correspond to a specific unlexicalized
 * parse tree, for use when the bottom-up parsing algorithm needs to generate
 * only the analyses that are consistent with a particular unlexicalized tree.
 * Accordingly, the individual <tt>Constraint</tt> objects in this set form
 * an isomorphic tree structure.
 */
public class UnlexTreeConstraintSet implements ConstraintSet {

  /**
   * The root of this tree of constraints.
   */
  protected UnlexTreeConstraint root;

  /**
   * The leaves of this tree of constraints.
   */
  protected List leaves = new ArrayList();

  public UnlexTreeConstraintSet() {
  }

  public UnlexTreeConstraintSet(Sexp tree) {
    buildConstraintSet(tree);
  }

  /**
   * Builds the constraint tree from the specified unlexicalized parse tree.
   * As a necessary side-effect, the {@link #root} and {@link #leaves} data
   * members will be set/populated by this method.
   *
   * @param tree the tree from which to build this constraint set
   */
  protected void buildConstraintSet(Sexp tree) {
    root = new UnlexTreeConstraint(tree);
    collectLeaves(root);
  }

  protected void collectLeaves(UnlexTreeConstraint tree) {
    if (tree.isLeaf())
      leaves.add(tree);
    else {
      List children = tree.getChildren();
      for (int i = 0; i < children.size(); i++)
        collectLeaves((UnlexTreeConstraint)children.get(i));
    }
  }

  // predicates
  /**
   * Returns <code>true</code>, since this type of constraint set does,
   * indeed, have a tree structure.
   *
   * @return <code>true</code>
   */
  public boolean hasTreeStructure() { return true; }

  /**
   * Returns <code>true</code>, since a satisfying constraint must be found
   * for every chart item.
   *
   * @return <code>true</code>
   */
  public boolean findAtLeastOneSatisfying() { return true; }

  /**
   * Returns <code>false</code>, since this type of constraint set guarantees
   * consistency among its constraints, meaning that, since every chart item
   * must have an assigned, satisfying constraint, there cannot be any
   * constraint violations, and therefore no such violation-checking needs
   * to occur during decoding.
   *
   * @return <code>false</code>
   *
   * @see #findAtLeastOneSatisyfing()
   */
  public boolean findNoViolations() { return false; }

  public Constraint root() { return root; }
  public List leaves() { return leaves; }

  /**
   * Simply throws an <code>UnsupportedOperationException</code>, since
   * violations do not need to be checked for this type of constraint set.
   *
   * @param item the item to be checked for violations
   * @return <code>true</code> if the specified item violates a constraint
   * in this set, <code>false</code> otherwise
   *
   * @see #findNoViolations()
   */
  public boolean containsViolation(Item item) {
    throw new UnsupportedOperationException();
  }

  public Constraint constraintSatisfying(Item item) {
    // make sure all children items have assigned constraints, and that
    // all assigned constraints of children items have the same parent
    // constraint
    CKYItem ckyItem = (CKYItem)item;

    // normally, preterminal items should be assigned constraints from the
    // list of leaves, and thus this case should normally not be considered;
    // but just in case a brain-dead programmer creates a decoder that is
    // inefficient in this way, here's the code to deal with it
    if (ckyItem.isPreterminal()) {
      int numLeaves = leaves.size();
      for (int i = 0; i < numLeaves; i++) {
        Constraint curr = (Constraint)leaves.get(i);
        if (curr.isSatisfiedBy(item))
          return curr;
      }
      return null;
    }

    // get the parent of the head child's constraint
    Constraint headChildConstraintParent =
      ckyItem.headChild().getConstraint().getParent();

    return (headChildConstraintParent.isSatisfiedBy(item) ?
            headChildConstraintParent : null);
  }

  /**
   * Test driver for this class.
   * @param args
   */
  public static void main(String[] args) {
    try {
      SexpTokenizer tok = new SexpTokenizer(System.in, Language.encoding(), 8192);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
        UnlexTreeConstraintSet set = new UnlexTreeConstraintSet(curr);
        System.out.println(((SexpConvertible)set.root()).toSexp());
      }
    }
    catch (Exception e) {
      System.err.println(e);
    }
  }
}