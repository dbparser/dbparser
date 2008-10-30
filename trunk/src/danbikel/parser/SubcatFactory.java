package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * Specification for a <code>Subcat</code> object factory, to be used by
 * the <code>Subcats</code> static factory class.
 *
 * @see Subcats
 * @see Settings#subcatFactoryClass
 */
public interface SubcatFactory extends Serializable {
  /**
   * Return a <code>Subcat</code> object created with its default constructor.
   */
  public Subcat get();
  /**
   * Return a <code>Subcat</code> object created with its one-argument
   * constructor, using the specified list.
   *
   * @param list a list containing only <code>Symbol</code> objects; the
   * behavior is undefined if <code>list</code> contains a <code>SexpList</code>
   * object
   */
  public Subcat get(SexpList list);
}
