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

  public WordList copy() {
    int size = data.length;
    WordList newList = new WordArrayList(size);
    for (int i = 0; i < size; i++)
      newList.set(i, getWord(i).copy());
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
