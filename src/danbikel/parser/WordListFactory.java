package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;
import java.util.*;

/**
 * Provides methods to create new <code>WordList</code> objects.  Depending
 * on the size given, different implementations of the <code>WordList</code>
 * interface are returned by the factory methods.
 */
public class WordListFactory {
  private WordListFactory() {}

  private final static WordList emptyWordList = new WordArrayList(0);

  /**
   * Returns a new <code>WordList</code> object of the specified size.
   * Different <code>WordList</code> implementations are returned depending
   * on whether the size is <tt>1</tt> or greater than <tt>1</tt>.  If the
   * size is <tt>0</tt>, a xanonical empty word list object is returned.
   *
   * @return a <code>WordList</code> object of the specified size
   */
  public static WordList newList(int size) {
    if (size < 0)
      throw new IllegalArgumentException();
    switch (size) {
    case 0:
      return emptyWordList;
    case 1:
      return new SingletonWordList(size);
    default:
      return new WordArrayList(size);
    }
  }

  /**
   * Returns a new <code>WordList</code> object containing <code>Word</code>
   * objects constructed from the elements of the specified list, using the
   * {@link Word#Word(Sexp)} constructor. Different <code>WordList</code>
   * implementations are returned depending on whether the size of the
   * specified list is <tt>1</tt> or greater than <tt>1</tt>.  If the size is
   * <tt>0</tt>, a canonical empty word list object is returned.
   *
   * @return a <code>WordList</code> object of the specified size
   */
  public static WordList newList(SexpList list) {
    int size = list.length();
    WordList newList = newList(size);
    for (int i = 0; i < size; i++)
      newList.set(i, Words.get(list.get(i)));
    return newList;
  }

  /**
   * Returns a new <code>WordList</code> object containing the words of the
   * specified collection.  Different <code>WordList</code> implementations
   * are returned depending on whether the size of the specified collection
   * is <tt>1</tt> or greater than <tt>1</tt>.  If the size is <tt>0</tt>, a
   * canonical empty word list object is returned.
   *
   * @return a <code>WordList</code> object of the specified size
   *
   * @throws ClassCastException if any of the objects in the specified
   * collection are not instances of <code>Word</code>
   */
  public static WordList newList(Collection c) {
    int size = c.size();
    switch (size) {
    case 0:
      return emptyWordList;
    case 1:
      return new SingletonWordList(c);
    default:
      return new WordArrayList(c);
    }
  }
}
