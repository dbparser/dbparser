package danbikel.lisp;

import java.io.Serializable;

/**
 * A simple interface to identify classes that have a method that converts
 * instances to <code>Sexp</code> objects.
 */
public interface SexpConvertible extends Serializable {
  /**
   * Converts this object to an S-expression.
   *
   * @return the S-expression that represents an instance of an implementor of
   * this class
   */
  public Sexp toSexp();
}
