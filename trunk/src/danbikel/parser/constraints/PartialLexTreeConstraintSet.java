package danbikel.parser.constraints;

import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

/**
 * Constructs a set of {@link PartialLexTreeConstraint}s for a given
 * tree.
 */
public class PartialLexTreeConstraintSet extends UnlexTreeConstraintSet {

  /**
   * Constructs an empty constraint set.
   */
  public PartialLexTreeConstraintSet() {
    super();
  }

  /**
   * Constructs a set of {@link PartialLexTreeConstraint} objects forming a tree
   * structure from the specified syntactic tree.  The specified tree is
   * lexicalized using a head finder.
   * @param tree the syntactic tree from which to construct a tree of
   * constraints
   */
  public PartialLexTreeConstraintSet(Sexp tree) {
    super(tree);
  }

  /**
   * Builds the constraint tree from the specified unlexicalized parse tree.
   * As a necessary side-effect, the {@link #root} and {@link #leaves} data
   * members will be set/populated by this method.
   *
   * @param tree the tree from which to build this constraint set
   */
  protected void buildConstraintSet(Sexp tree) {
    root = new PartialLexTreeConstraint(tree);
    collectNodes(root);
    list.trimToSize();
    leaves.trimToSize();
  }

  /**
   * Test driver for this class.
   * @param args
   */
  public static void main(String[] args) {
    try {
      SexpTokenizer tok =
	new SexpTokenizer(System.in, Language.encoding(), 8192);
      Sexp curr = null;
      while ((curr = Sexp.read(tok)) != null) {
	PartialLexTreeConstraintSet set = new PartialLexTreeConstraintSet(curr);
	System.out.println(((SexpConvertible)set.root()).toSexp());
      }
    }
    catch (Exception e) {
      System.err.println(e);
    }
  }
}
