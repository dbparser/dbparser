package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;

/**
 * A default implementation of the {@link Shift} interface that simply shifts
 * every modifier or word, skipping nothing.  That is, the {@link
 * #shift(TrainerEvent,SexpList,Sexp)} method shifts the specified
 * previous modifier into the specified previous modifier list, the {@link
 * #shift(TrainerEvent,WordList,Word)} method shifts the specified word
 * into the specified previous word list and the two skip methods both return
 * false regardless of the values of their arguments.
 */
public class DefaultShifter implements Shift {
  /** Default constructor. */
  public DefaultShifter() {}

  public void shift(TrainerEvent event, SexpList list, Sexp prevMod) {
    list.remove(list.length() - 1);
    list.add(0, prevMod);
  }
  public void shift(TrainerEvent event, WordList wordList, Word prevWord) {
    wordList.shift(prevWord);
  }
  public boolean skip(Item item, Sexp prevMod) {
    return false;
  }
  public boolean skip(Item item, Word prevWord) {
    return false;
  }
}
