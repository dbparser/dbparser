package danbikel.parser;

import danbikel.lisp.*;

/**
 * Contains static constants for use by this package.
 */
public class Constants {
  /** Private constructor: this class is only to be used for static data. */
  private Constants() {}

  /** The constant representing the left side or the left-to-right direction. */
  public static final boolean LEFT = false;
  /** The constant representing the right side or the right-to-left
      direction. */
  public static final boolean RIGHT = true;
  /** The symbol constant representing the left side or the
      left-to-right direction. */
  public static final Symbol leftSym = Symbol.add("left");
  /** The symbol constant representing the right side or the right-to-left
      direction. */
  public static final Symbol rightSym = Symbol.add("right");

  /** A symbol constant to represent truth. */
  public static final Symbol trueSym = Symbol.add("true");
  /** A symbol constant to represent falsity. */
  public static final Symbol falseSym = Symbol.add("false");

  /**
   * A constant to represent the logarithm of zero, equal to
   * <code>Double#NEGATIVE_INFINITY</code>.
   */
  public static final double logOfZero = Double.NEGATIVE_INFINITY;

  /**
   * The default file buffer size, which may be passed as an argument to
   * <code>BufferedReader</code> and <code>BufferedWriter</code> constructors.
   */
  public static final int defaultFileBufsize = 81920;

  /**
   * Converts a boolean value into a symbol representation.
   */
  public final static Symbol booleanToSym(boolean value) {
    return (value ? Constants.trueSym : Constants.falseSym);
  }
}
