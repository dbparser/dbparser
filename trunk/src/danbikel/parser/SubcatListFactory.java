package danbikel.parser;

import danbikel.lisp.*;

/**
 * A factory for creating <code>SubcatList</code> objects.
 *
 * @see SubcatList
 * @see Subcats
 * @see Settings#subcatFactoryClass
 */
public class SubcatListFactory implements SubcatFactory {
  /** Constructs a new <code>SubcatListFactory</code>. */
  public SubcatListFactory() {}

  /** Returns an empty <code>SubcatList</code>. */
  public Subcat get() { return new SubcatList(); }

  /**
   * Returns a <code>SubcatList</code> initialized with the requirements
   * contained in the specified list.
   *
   * @param list a list of <code>Symbol</code> objects to be added
   * as requirements to a new <code>SubcatList</code> instance
   */
  public Subcat get(SexpList list) { return new SubcatList(list); }  
}
