package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;

/**
 * An implementation of the <tt>Shift</tt> interface that does not shift
 * punctuation into the history when the current parent node label is that of
 * a base NP.
 *
 * @see Treebank#isBaseNP(Symbol)
 * @see Treebank#isPunctuation(Symbol)
 */
public class BaseNPAwareShifter implements Shift {
  public BaseNPAwareShifter() {}


  /**
   * The previous modifier is <i>not</i> shifted into the history if the
   * current parent (as determined by {@link TrainerEvent#parent()}) is a base
   * NP and the previous modifier is punctuation.
   *
   * @param event the <tt>TrainerEvent</tt> whose history is to be updated
   * @param list the current history of previously-generated modifiers
   * @param prevMod the previously-generated modifier
   *
   * @see Treebank#isBaseNP(Symbol)
   * @see Treebank#isPunctuation(Symbol)
   */
  public void shift(TrainerEvent event, SexpList list, Sexp prevMod) {
    Symbol prevModSym = prevMod.symbol();

    boolean doShift;
    // do not treat punctuation as a true "previous modifier" when inside
    // a base NP
    if (event != null && Language.treebank.isBaseNP(event.parent())) { 
      doShift = !Language.treebank.isPunctuation(prevModSym);
    }
    else {
      doShift = true;
    }
    if (doShift) {
      list.remove(list.length() - 1);
      list.add(0, prevModSym);
    }
  }

  /**
   * The head word of the previous modifier is <i>not</i> shifted into the
   * history if the current parent (as determined by {@link
   * TrainerEvent#parent()}) is a base NP and the previous modifier is
   * punctuation.
   *
   * @param event the <tt>TrainerEvent</tt> whose history is to be updated
   * @param wordList the current history of previously-generated modifier
   *                 head words
   * @param prevWord the head word of the previously-generated modifier
   *
   * @see Treebank#isBaseNP(Symbol)
   * @see Treebank#isPunctuation(Symbol)
   */
  public void shift(TrainerEvent event, WordList wordList, Word prevWord) {
    boolean doShift;
    // do not treat punctuation as a true "previous word" when inside
    // a base NP
    if (event != null && Language.treebank.isBaseNP(event.parent())) {
      doShift = !Language.treebank.isPunctuation(prevWord.tag());
    }
    else {
      doShift = true;
    }
    if (doShift)
      wordList.shift(prevWord);
  }


  /**
   * The previous modifier is skipped (not included in the construction of the
   * history) if the current parent (as determined by {@link CKYItem#label()})
   * is a base NP and the previous modifier is punctuation.
   *
   * @param item the <tt>CKYItem</tt> object whose history is being constructed
   * @param prevMod the previously-generated modifier
   * @return whether or not to skip the specified previous modifier when
   * constructing the modifier history for the specified chart item
   *
   * @see Treebank#isBaseNP(Symbol)
   * @see Treebank#isPunctuation(Symbol)
   */
  public boolean skip(Item item, Sexp prevMod) {
    Symbol prevModSym = prevMod.symbol();
    // do not treat punctuation as a true "previous word" when inside
    // a base NP
    if (Language.treebank.isBaseNP((Symbol)item.label()))
      return Language.treebank.isPunctuation(prevModSym);
    else return false;
  }

  /**
   * The head word of the previous modifier is skipped (not included in the
   * construction of the history) ifthe current parent (as determined by {@link
   * CKYItem#label()}) is a base NP and the previous modifier is punctuation.
   *
   * @param item the <tt>CKYItem</tt> object whose history is being constructed
   * @param prevWord the head word of the previously-generated modifier
   * @return whether or not to skip the specified previous modifier's head word
   * when constructing the modifier head word history for the specified
   * chart item
   *
   * @see Treebank#isBaseNP(Symbol)
   * @see Treebank#isPunctuation(Symbol)
   */
  public boolean skip(Item item, Word prevWord) {
    // do not treat punctuation as a true "previous word" when inside
    // a base NP
    if (Language.treebank.isBaseNP((Symbol)item.label()))
      return Language.treebank.isPunctuation(prevWord.tag());
    else return false;
  }
}
