package danbikel.parser.constraints;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

/**
 * Specifies a node in a tree of constraints, to allow the decoder only to
 * pursue theories that are consistent with a particular head-lexicalized
 * tree.
 * <br>
 * <b>Implementation note</b>: The public constructor of this class expects a
 * <code>Sexp</code> object representing an unlexicalized parse tree, which
 * is lexicalized using the current {@linkplain Language#headFinder()
 * head finder}.
 */
public class LexTreeConstraint extends UnlexTreeConstraint {

  /**
   * The head word associated with this constraint.
   */
  protected Word headWord;

  /**
   * Constructs a tree of constraints that is isomorphic to the specified
   * parse tree.  Head lexicalization of the specified parse tree is performed
   * &quot;on the fly&quot; using the current {@linkplain Language#headFinder()
   * head finder}.
   *
   * @param tree the parse tree from which to construct an isomorphic tree
   * of <code>LexTreeConstraint</code> nodes
   */
  public LexTreeConstraint(Sexp tree) {
    this(null, tree, new IntCounter(0), Language.headFinder());
  }

  /**
   * Constructs a tree of constraints that is isomorphic to the specified
   * parse tree.
   *
   * @param parent the parent constraint node of this node, or <code>null</code>
   * if this node is the root of the constraint tree
   * @param tree the (unlexicalized) parse tree from which to construct
   * an isomorphic tree of constraints
   * @param currWordIdx the zero-based index of the current word, as determined
   * by the left-to-right, top-down traversal of the specified tree
   * @param headFinder the <code>HeadFinder</code> instance to use to
   * lexicalize the specified parse tree
   */
  protected LexTreeConstraint(LexTreeConstraint parent,
			      Sexp tree, IntCounter currWordIdx,
			      HeadFinder headFinder) {
    if (Language.treebank().isPreterminal(tree)) {
      this.parent = parent;
      children = Collections.EMPTY_LIST;
      headWord = Language.treebank().makeWord(tree);
      label = headWord.tag();
      start = end = currWordIdx.get();
      currWordIdx.increment();
    }
    else {
      SexpList treeList = tree.list();
      int treeListLen = treeList.length();

      //label = Language.treebank.getCanonical(treeList.symbolAt(0));
      label = treeList.symbolAt(0);
      start = currWordIdx.get();
      this.parent = parent;
      children = new ArrayList(treeListLen - 1);
      for (int i = 1; i < treeListLen; i++) {
	Sexp child = treeList.get(i);
	children.add(new LexTreeConstraint(this, child, currWordIdx,
					   headFinder));
      }
      end = currWordIdx.get() - 1;

      // inherit head word from head child in constraint tree
      int headIdx = headFinder.findHead(tree) - 1; // convert to zero-based
      headWord = ((LexTreeConstraint)children.get(headIdx)).headWord;
    }
  }

  /**
   * Returns <code>true</code> if this constraint {@linkplain
   * #isLocallySatisfiedBy is locally satisfied by} the specified item and
   * if this constraint's {@linkplain #spanMatches span matches} that of
   * the specified item.  This overridden definition is in stark contrast
   * to that of {@link UnlexTreeConstraint}, where preterminals are
   * <i>always</i> satisfied by preterminal constraints, meaning that
   * parts of speech are not constrained.
   *
   * @param item the item to be tested
   * @return <code>true</code> if this constraint {@linkplain
   * #isLocallySatisfiedBy is locally satisfied by} the specified item and if
   * this constraint's {@linkplain #spanMatches span matches} that of the
   * specified item.
   */
  protected boolean isSatisfiedByPreterminal(CKYItem item) {
    if (isLocallySatisfiedBy(item) && spanMatches(item)) {
      satisfied = true;
      return true;
    }
    else
      return false;
  }


  public boolean isLocallySatisfiedBy(Item item) {
    return item.label() == label && ((CKYItem)item).headWord().equals(headWord);
  }

  public Sexp toSexp() {
    SexpList retVal = new SexpList(children.size() + 1);

    Symbol thisNode = Symbol.add(label.toString() + headWord + "-" +
				 start + "-" + end);

    retVal.add(thisNode);
    for (int i = 0; i < children.size(); i++)
      retVal.add(((SexpConvertible)children.get(i)).toSexp());

    return retVal;
  }

  public String toString() {
    return "headWord=" + headWord + ", label=" + label +
	   ", span=(" + start + "," + end + "), parentLabel=" +
	   (parent == null ? "null" : parent.label.toString());
  }
}