package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;

/**
 * A default implementation of the {@link Shift} interface that simply shifts
 * every modifier or word, skipping nothing.  That is, the {@link
 * #shift(TrainerEvent,SexpList,Sexp,Sexp)} method shifts the specified
 * previous modifier into the specified previous modifier list, the {@link
 * #shift(TrainerEvent,WordList,Word,Word)} method shifts the specified word
 * into the specified previous word list and the two skip methods both return
 * false regardless of the values of their arguments.
 */
public class DefaultShifter implements Shift {
  /** Default constructor. */
  public DefaultShifter() {}

  public void shift(TrainerEvent event, SexpList list,
		    Sexp prevMod, Sexp currMod) {
    list.remove(list.length() - 1);
    list.add(0, prevMod);
  }
  public void shift(TrainerEvent event, WordList wordList,
		    Word prevWord, Word currWord) {
    wordList.shift(prevWord);
  }
  public boolean skip(Item item, Sexp prevMod, Sexp currMod) {
    return false;
  }
  public boolean skip(Item item, Word prevWord, Word currWord) {
    return false;
  }




  public void shift(TrainerEvent event, SexpList list, Sexp sexp) {
    list.remove(list.length() - 1);
    list.add(0, sexp);
  }
  public void shift(TrainerEvent event, WordList wordList, Word word) {
    wordList.shift(word);
  }
  public boolean skip(CKYItem item, Sexp sexp) {
    return false;
  }
  public boolean skip(CKYItem item, Word word) {
    return false;
  }
}
