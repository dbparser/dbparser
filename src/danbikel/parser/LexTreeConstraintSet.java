package danbikel.parser;

import danbikel.lisp.*;
import java.util.*;

public class LexTreeConstraintSet extends UnlexTreeConstraintSet {

  public LexTreeConstraintSet() {
    super();
  }

  public LexTreeConstraintSet(Sexp tree) {
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
    root = new LexTreeConstraint(tree);
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
	LexTreeConstraintSet set = new LexTreeConstraintSet(curr);
	System.out.println(((SexpConvertible)set.root()).toSexp());
      }
    }
    catch (Exception e) {
      System.err.println(e);
    }
  }
}