package danbikel.parser;

import danbikel.util.*;
import danbikel.lisp.*;

/**
 * An implementation of the <tt>Shift</tt> interface that does not shift
 * punctuation into the history when the current parent node label is that of
 * a base NP.  Furthermore, regardless of the parent node label, this shifter
 * treats punctuation and conjunction modifiers as conditionally independent,
 * by not shifting one into the history when it is followed by the other.
 *
 * @see Treebank#baseNPLabel()
 * @see Treebank#isPunctuation(Symbol)
 * @see Treebank#isConjunction(Symbol)
 */
public class BaseNPAwareShifter implements Shift {
  /**
   * The value of {@link Treebank#baseNPLabel}, cached for efficiency and
   * convenience.
   */
  private final Symbol baseNP = Language.treebank.baseNPLabel();

  public BaseNPAwareShifter() {}


  /**
   * The previous modifier is <i>not</i> shifted into the history if any
   * of the following conditions is true:
   * <ul>
   * <li>the current parent (as determined by {@link TrainerEvent#parent()})
   * is a base NP and the previous modifier is punctuation
   * <li>the previous modifier is punctuation and the current modifier is
   * a conjunction
   * <li>the previous modifier is a conjunction and the current modifier is
   * punctuation
   * </ul>
   *
   * @param event the <tt>TrainerEvent</tt> whose history is to be updated
   * @param list the current history of previously-generated modifiers
   * @param prevMod the previously-generated modifier
   * @param currMod the modifier that has just been generated
   *
   * @see Treebank#baseNPLabel()
   * @see Treebank#isPunctuation(Symbol)
   * @see Treebank#isConjunction(Symbol)
   */
  public void shift(TrainerEvent event, SexpList list,
		    Sexp prevMod, Sexp currMod) {
    Symbol currModSym = currMod.symbol();
    Symbol prevModSym = prevMod.symbol();

    boolean doShift;
    // do not treat punctuation as a true "previous modifier" when inside
    // a base NP
    if (event != null && event.parent() == baseNP) {
      doShift = !Language.treebank.isPunctuation(prevModSym);
    }
    else {
      // treat punctuation and conjunction as conditionally independent:
      // if one follows the other, the previous element is *not* shifted
      // into the history list
      /*
      doShift = !((Language.treebank.isPunctuation(prevModSym) &&
		   Language.treebank.isConjunction(currModSym)) ||
		  (Language.treebank.isConjunction(prevModSym) &&
		   Language.treebank.isPunctuation(currModSym)));
      */
      doShift = true;
    }
    if (doShift) {
      list.remove(list.length() - 1);
      list.add(0, prevModSym);
    }
  }

  /**
   * The head word of the previous modifier is <i>not</i> shifted into the
   * history if any of the following conditions is true:
   * <ul>
   * <li>the current parent (as determined by {@link TrainerEvent#parent()})
   * is a base NP and the previous modifier is punctuation
   * <li>the tag of the previous modifier's head word is punctuation and the
   * tag of the current modifier's head word is a conjunction
   * <li>the tag of the previous modifier's head word is a conjunction and
   * the tag of the current modifier's head word is punctuation
   * </ul>
   *
   * @param event the <tt>TrainerEvent</tt> whose history is to be updated
   * @param wordList the current history of previously-generated modifiers
   * @param prevWord the head word of the previously-generated modifier
   * @param currWord the head word of the modifier that has just been generated
   *
   * @see Treebank#baseNPLabel()
   * @see Treebank#isPunctuation(Symbol)
   * @see Treebank#isConjunction(Symbol)
   */
  public void shift(TrainerEvent event, WordList wordList,
		    Word prevWord, Word currWord) {
    boolean doShift;
    // do not treat punctuation as a true "previous word" when inside
    // a base NP
    if (event != null && event.parent() == baseNP) {
      doShift = !Language.treebank.isPunctuation(prevWord.tag());
    }
    else {
      // treat punctuation and conjunction as conditionally independent:
      // if one follows the other, the previous element is *not* shifted
      // into the history list
      /*
      doShift = !((Language.treebank.isPunctuation(prevWord.tag()) &&
		   Language.treebank.isConjunction(currWord.tag())) ||
		  (Language.treebank.isConjunction(prevWord.tag()) &&
		   Language.treebank.isPunctuation(currWord.tag())));
      */
      doShift = true;
    }
    if (doShift)
      wordList.shift(prevWord);
  }


  /**
   * The previous modifier is skipped (not included in the construction of
   * the history) if any of the following conditions is true:
   * <ul>
   * <li>the current parent (as determined by {@link CKYItem#label()})
   * is a base NP and the previous modifier is punctuation
   * <li>the previous modifier is punctuation and the current modifier is
   * a conjunction
   * <li>the previous modifier is a conjunction and the current modifier is
   * punctuation
   * </ul>
   *
   * @param item the <tt>CKYItem</tt> object whose history is being constructed
   * @param prevMod the previously-generated modifier
   * @param currMod the modifier that has just been generated
   * @return whether or not to skip the specified previous modifier when
   * constructing the modifier history for the specified chart item
   *
   * @see Treebank#baseNPLabel()
   * @see Treebank#isPunctuation(Symbol)
   * @see Treebank#isConjunction(Symbol)
   */
  public boolean skip(Item item, Sexp prevMod, Sexp currMod) {
    Symbol currModSym = currMod.symbol();
    Symbol prevModSym = prevMod.symbol();
    // do not treat punctuation as a true "previous word" when inside
    // a base NP
    if (item.label() == baseNP)
      return Language.treebank.isPunctuation(prevModSym);
    // treat punctuation and conjunction as conditionally independent:
    // if one follows the other, the previous element is *not* shifted
    // into the history list
    /*
    else return ((Language.treebank.isPunctuation(prevModSym) &&
		  Language.treebank.isConjunction(currModSym)) ||
		 (Language.treebank.isConjunction(prevModSym) &&
		  Language.treebank.isPunctuation(currModSym)));
    */
    else return false;
  }

  /**
   * The head word of the previous modifier is skipped (not included in the
   * construction of the history) if any of the following conditions is true:
   * <ul>
   * <li>the current parent (as determined by {@link CKYItem#label()})
   * is a base NP and the previous modifier is punctuation
   * <li>the tag of the previous modifier's head word is punctuation and the
   * tag of the current modifier's head word is a conjunction
   * <li>the tag of the previous modifier's head word is a conjunction and the
   * tag of the current modifier's head word is punctuation
   * </ul>
   *
   * @param item the <tt>CKYItem</tt> object whose history is being constructed
   * @param prevWord the head word of the previously-generated modifier
   * @param currWord the head word of the modifier that has just been generated
   * @return whether or not to skip the specified previous modifier's head word
   * when constructing the modifier head word history for the specified
   * chart item
   *
   * @see Treebank#baseNPLabel()
   * @see Treebank#isPunctuation(Symbol)
   * @see Treebank#isConjunction(Symbol)
   */
  public boolean skip(Item item, Word prevWord, Word currWord) {
    // do not treat punctuation as a true "previous word" when inside
    // a base NP
    if (item.label() == baseNP)
      return Language.treebank.isPunctuation(prevWord.tag());
    // treat punctuation and conjunction as conditionally independent:
    // if one follows the other, the previous element is *not* shifted
    // into the history list
    /*
    else return ((Language.treebank.isPunctuation(prevWord.tag()) &&
		  Language.treebank.isConjunction(currWord.tag())) ||
		 (Language.treebank.isConjunction(prevWord.tag()) &&
		  Language.treebank.isPunctuation(currWord.tag())));
    */
    else return false;
  }
}
