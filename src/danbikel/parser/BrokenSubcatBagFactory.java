package danbikel.parser;

import danbikel.lisp.*;

/**
 * A factory for creating <code>BrokenSubcatBag</code> objects.
 *
 * @see BrokenSubcatBag
 * @see Subcats
 * @see Settings#subcatFactoryClass
 */
public class BrokenSubcatBagFactory implements SubcatFactory {
  /** Constructs a new <code>SubcatBagFactory</code>. */
  public BrokenSubcatBagFactory() {}

  /** Returns an empty <code>SubcatBag</code>. */
  public Subcat get() { return new BrokenSubcatBag(); }

  /**
   * Returns a <code>SubcatBag</code> initialized with the requirements
   * contained in the specified list.
   *
   * @param list a list of <code>Symbol</code> objects to be added
   * as requirements to a new <code>SubcatBag</code> instance
   */
  public Subcat get(SexpList list) { return new BrokenSubcatBag(list); }
}
