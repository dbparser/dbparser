package danbikel.parser;

import danbikel.lisp.*;

/**
 * Representation of the complete back-off structure of the generation model
 * for modifying nonterminals/part-of-speech tags (the modifying nonterminals
 * are partially lexicalized with the parts of speech of their respective
 * head words).
 * <p>
 * <b>It is a horrendous bug that all of these <code>ProbabilityStructure</code>
 * classes do not copy various lists from the <code>TrainerEvent</code> objects
 * before removing gap augmentations from their elements.</b>
 * <p>
 */
public class ModNonterminalModelStructure3 extends ProbabilityStructure {
  // data members
  private Symbol startSym = Language.training().startSym();
  private Word startWord = Language.training().startWord();
  private Symbol baseNP = Language.treebank().baseNPLabel();

  public ModNonterminalModelStructure3() {
    super();
  }

  public int maxEventComponents() { return 8; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;

    if (modEvent.parent() == baseNP)
      return getBaseNPHistory(modEvent, backOffLevel);

    Symbol side = Constants.sideToSym(modEvent.side());

    MutableEvent hist = historiesWithSubcats[backOffLevel];

    hist.clear();
    Symbol verbInterveningSym =
      Constants.booleanToSym(modEvent.verbIntervening());
    switch (backOffLevel) {
    case 0:
      // for p(M(t)_i | P, H, w, t, verbIntervening, (M_i-1,...,M_i-k), subcat,
      //                side)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().word());
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, Language.training.removeGapAugmentation(modEvent.previousMods()));
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 1:
      // for p(M(t)_i | P, H, t, verbIntervening, M_i-1, subcat, side)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, Language.training.removeGapAugmentation(modEvent.previousMods().get(0)));
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    case 2:
      // for p(M(t)_i | P, H, verbIntervening, map(M_i-1), subcat, side)
      Symbol mappedPrevMod =
	Collins.mapPrevMod(modEvent.previousMods().symbolAt(0));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevMod);
      hist.add(1, modEvent.subcat());
      hist.add(0, side);
      break;
    }
    return hist;
  }

  private Event getBaseNPHistory(ModifierEvent modEvent, int backOffLevel) {
    MutableEvent hist = histories[backOffLevel];

    Symbol side = Constants.sideToSym(modEvent.side());

    Symbol prevModLabel =
      (modEvent.previousMods().get(0) == startSym ?
       modEvent.head() : modEvent.previousMods().symbolAt(0));
    Word prevModWord =
      (modEvent.previousWords().getWord(0).equals(startWord) ?
       modEvent.headWord() : modEvent.previousWords().getWord(0));
    hist.clear();
    switch (backOffLevel) {
    case 0:
      // for p(M(t)_i | P, M(w,t)_i-1, side)
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.word());
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
    case 1:
      // for p(M(t)_i | P, M(t)_i-1, side)
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.tag());
      hist.add(side);
      break;
    case 2:
      // for p(M(t)_i | P, M_i-1, side)
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(side);
      break;
    }
    return hist;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(modEvent.modifier());
    future.add(modEvent.modHeadWord().tag());
    return future;
  }

  public ProbabilityStructure copy() {
    return new ModNonterminalModelStructure3();
  }
}
