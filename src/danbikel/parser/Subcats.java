package danbikel.parser;

import danbikel.lisp.*;
import java.io.Serializable;

/**
 * Static factory for <code>Subcat</code> objects.  This scheme allows
 * the type of <code>Subcat</code> object to be determined at run-time.
 * The type of subcat factory used is deteremined by the value of the
 * property {@link Settings#subcatFactoryClass}.
 *
 * @see SubcatFactory
 * @see Settings#subcatFactoryClass
 * @see Subcat
 */
public class Subcats implements Serializable {
  private static final String className = Subcats.class.getName();


  private Subcats() {}

  private static SubcatFactory factory;
  static {
    String subcatFactStr = Settings.get(Settings.subcatFactoryClass);
    if (subcatFactStr != null) {
      try {
	factory = (SubcatFactory)Class.forName(subcatFactStr).newInstance();
      }
      catch (Exception e) {
	System.err.println(className + ": error creating " +
			   "instance of " + subcatFactStr + ":\n\t" + e +
			   "\n\tusing SubcatBagFactory instead");
	factory = new SubcatBagFactory();
      }
    }
    else {
      System.err.println(className + ": error: the property " +
			 Settings.subcatFactoryClass + " was not set;\n\t" +
			 "using SubcatBagFactory");
      factory = new SubcatBagFactory();
    }
  }

  /**
   * Return a <code>Subcat</code> object created with its default constructor.
   */
  public static Subcat get() {
    return factory.get();
  }
  /**
   * Return a <code>Subcat</code> object created with its one-argument
   * constructor, using the specified list.
   *
   * @param list a list containing only <code>Symbol</code> objects; the
   * behavior is undefined if <code>list</code> contains a <code>SexpList</code>
   * object
   */
  public static Subcat get(SexpList list) {
    return factory.get(list);
  }
}
