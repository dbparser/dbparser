package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * Representation of all possible data present in a complex
 * nonterminal annotation: the base label, any augmentations and any index.
 * Not following the traditional object-oriented design principles, this class
 * is used strictly for representing the data present in a complex nonterminal,
 * but that data is easily manipulated by two methods of the {@link Treebank}
 * class, <code>Treebank.parseNonterminal</code> and
 * <code>Treebank.addAugmentation</code>.
 *
 * @see Treebank#parseNonterminal(Symbol,Nonterminal)
 * @see Treebank#addAugmentation(Nonterminal,Symbol)
 */
public class Nonterminal implements Serializable {
  /** The unaugmented base nonterminal. */
  public Symbol base;
  /** A list of symbols representing any augmentations and delimiters. */
  public SexpList augmentations;
  /** The index of the augmented nonterminal, or -1 if none was present. */
  public int index;

  /**
   * Default constructor sets the <code>base</code> data member to be
   * <code>null</code>, the <code>augmentations</code> data member to be
   * a list with no elements and the <code>index</code> data member to be
   * -1.
   */
  public Nonterminal() {
    this(null, new SexpList(), -1);
  }

  /** Sets the data members of this new object to the specified values */
  public Nonterminal(Symbol base, SexpList augmentations, int index) {
    this.base = base;
    this.augmentations = augmentations;
    this.index = index;
  }

  /**
   * Returns a string representation of the nonterminal, identical the
   * original nonterminal that was parsed to create this object.
   */
  public String toString() {
    if (base == null || augmentations == null)
      return "null";
    int len = base.toString().length();
    int augLen = augmentations.length();
    // efficiency: don't need StringBuffer if all this has is a base
    if (augLen == 0 && index == -1)
      return base.toString();
    for (int i = 0; i < augLen; i++)
      len += augmentations.symbolAt(i).toString().length();
    StringBuffer sb = new StringBuffer(len + 3);
    sb.append(base);
    for (int i = 0; i < augLen; i++)
      sb.append(augmentations.get(i));
    if (index != -1)
      sb.append(index);
    return sb.toString();
  }

  /** Returns the symbol representing this complex nonterminal. */
  public Symbol toSymbol() {
    if (augmentations.length() == 0 && index == -1)
      return base;
    else
      return Symbol.get(toString());
  }
}

