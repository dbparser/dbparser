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

  public WordList copy() {
    WordList newList = new SingletonWordList(1);
    newList.set(0, getWord(0).copy());
    return newList;
  }

  public Sexp toSexp() {
    return (new SexpList(1)).add(getWord(0).toSexp());
  }
}
