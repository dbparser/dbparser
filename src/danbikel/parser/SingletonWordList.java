package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;

class SingletonWordList extends FixedSizeSingletonList
  implements WordList {

  public SingletonWordList(int size) {
    super(size);
  }
  public SingletonWordList(Collection c) {
    super(c);
  }

  public Word getWord(int index) {
    return (Word)get(index);
  }

  /**
   * Returns a copy of this list.
   * <p>
   * <b>Warning</b>: The returned copy is <i>not</i> a deep copy.  That is,
   * the <code>Word</code> object is simply a reference to the original object
   * from this list.
   */
  public WordList copy() {
    WordList newList = new SingletonWordList(1);
    //newList.set(0, getWord(0).copy());
    newList.set(0, getWord(0));
    return newList;
  }

  public Sexp toSexp() {
    return (new SexpList(1)).add(getWord(0).toSexp());
  }
}
