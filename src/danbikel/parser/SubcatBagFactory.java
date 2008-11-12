package danbikel.parser;

import danbikel.lisp.*;

/**
 * A factory for creating <code>SubcatBag</code> objects.
 *
 * @see SubcatBag
 * @see Subcats
 * @see Settings#subcatFactoryClass
 */
public class SubcatBagFactory implements SubcatFactory {

  private SubcatBagInfo subcatBagInfo;

  /**
   * Constructs a new <code>SubcatBagFactory</code>.
   * @param rt the runtime for this subcat factory
   */
  public SubcatBagFactory(Runtime rt) {
    subcatBagInfo = new SubcatBagInfo(rt);
  }

  /** Returns an empty <code>SubcatBag</code>. */
  public Subcat get() {
    return new SubcatBag(subcatBagInfo);
  }

  /**
   * Returns a <code>SubcatBag</code> initialized with the requirements
   * contained in the specified list.
   *
   * @param list a list of <code>Symbol</code> objects to be added
   * as requirements to a new <code>SubcatBag</code> instance
   */
  public Subcat get(SexpList list) {
    return new SubcatBag(subcatBagInfo, list);
  }

  public void init(CountsTable nonterminals) {
    subcatBagInfo.setUpFastUidMap(nonterminals);
  }
}
