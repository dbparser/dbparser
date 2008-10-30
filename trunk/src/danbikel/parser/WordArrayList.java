package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;

class WordArrayList extends FixedSizeArrayList implements WordList {
  public WordArrayList(int size) {
    super(size);
  }
  public WordArrayList(Collection c) {
    super(c);
  }

  public Word getWord(int index) {
    return (Word)get(index);
  }

  /**
   * Returns a copy of this list.
   * <p>
   * <b>Warning</b>: The returned copy is <i>not</i> a deep copy.  That is,
   * the <code>Word</code> objects in the returned list are not copies of the
   * <code>Word</code> objects from the this list.
   */
  public WordList copy() {
    int size = data.length;
    WordList newList = new WordArrayList(size);
    for (int i = 0; i < size; i++)
      //newList.set(i, getWord(i).copy());
      newList.set(i, getWord(i));
    return newList;
  }

  public Sexp toSexp() {
    int size = size();
    if (size == 0)
      return SexpList.emptyList;
    SexpList newList = new SexpList(size);
    for (int i = 0; i < size; i++)
      newList.add(getWord(i).toSexp());
    return newList;
  }
}
