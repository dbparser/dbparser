package danbikel.parser.constraints;

import danbikel.util.*;
import danbikel.lisp.*;
import danbikel.parser.*;
import java.util.*;

/**
 * Represents a node in a parsing constraint tree, that requires an associated
 * chart item to have the same label, head word and head tag.  The crucial
 * difference between this type of constraint and {@link LexTreeConstraint}
 * is that the latter uses the {@link Word#equals} method to determine
 * lexicalized node equality, whereas this constraint explicitly compares only
 * the corresponding {@link Word#word()} and {@link Word#tag()} fields
 * of head words, making this type of constraint suitable when the head
 * word objects are subclasses of <code>Word</code> that include more
 * information (such as, for example, WordNet synsets).
 */
public class PartialLexTreeConstraint extends UnlexTreeConstraint {

  /**
   * The head word associated with this constraint.
   */
  protected Word headWord;

  /**
   * Constructs a tree of constraints given the specified parse tree.
   * @param tree
   */
  public PartialLexTreeConstraint(Sexp tree) {
    this(null, tree, new IntCounter(0), Language.headFinder());
  }

  protected PartialLexTreeConstraint(PartialLexTreeConstraint parent,
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
	children.add(new PartialLexTreeConstraint(this, child, currWordIdx,
						  headFinder));
      }
      end = currWordIdx.get() - 1;

      // inherit head word from head child in constraint tree
      int headIdx = headFinder.findHead(tree) - 1; // convert to zero-based
      headWord = ((PartialLexTreeConstraint)children.get(headIdx)).headWord;
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
    if (item.label() != label)
      return false;

    // COMPARE ONLY WORD AND TAG
    // (this is the "partial" of "PartialLexTreeConstraint")
    Word otherHeadWord = ((CKYItem)item).headWord();
    return (otherHeadWord.tag() == headWord.tag() &&
	    (otherHeadWord.word() == headWord.word()));
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
    return "headWord.word=" + headWord.word() + ", headWord.tag=" +
	   headWord.tag() + ", label=" + label +
	   ", span=(" + start + "," + end + "), parentLabel=" +
	   (parent == null ? "null" : parent.label.toString());
  }
}
