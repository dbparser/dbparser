package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;

/**
 * An interface to specify a fixed-size list of <code>Word</code> objects.
 */
public interface WordList extends FixedSizeList, SexpConvertible {
  /**
   * Gets the <code>Word</code> object at the specified index.
   *
   * @return the <code>Word</code> object at the specified index.
   */
  public Word getWord(int index);

  /**
   * Returns a deep copy of this word list.
   *
   * @return a deep copy of this word list.
   */
  public WordList copy();
}
