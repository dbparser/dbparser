package danbikel.parser;

import danbikel.lisp.*;

public class ModWordModelStructure2 extends ProbabilityStructure {
  // data members
  private Symbol startSym = Language.training().startSym();
  private Word startWord = Language.training().startWord();
  private Symbol baseNP = Language.treebank().baseNPLabel();

  public ModWordModelStructure2() {
    super();
  }

  public int maxEventComponents() { return 9; }
  public int numLevels() { return 3; }

  public Event getHistory(TrainerEvent trainerEvent, int backOffLevel) {
    ModifierEvent modEvent = (ModifierEvent)trainerEvent;

    if (modEvent.parent() == baseNP)
      return getBaseNPHistory(modEvent, backOffLevel);

    MutableEvent hist = historiesWithSubcats[backOffLevel];
    hist.clear();
    Symbol verbInterveningSym =
      Constants.booleanToSym(modEvent.verbIntervening());
    Symbol mappedPrevModSym =
      Collins.mapPrevMod(modEvent.previousMods().symbolAt(0));

    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, H, w, t, verbIntervening, map(M_i-1),  subcat)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().word());
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      break;
    case 1:
      // for p(w_i | M(t)_i, P, H, t, verbIntervening, map(M_i-1), subcat)
      hist.add(0, Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(0, modEvent.modHeadWord().tag());
      hist.add(0, Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(0, Language.training.removeGapAugmentation(modEvent.head()));
      hist.add(0, modEvent.headWord().tag());
      hist.add(0, verbInterveningSym);
      hist.add(0, mappedPrevModSym);
      hist.add(1, modEvent.subcat());
      break;
    case 2:
      // for p(w_i | t_i)
      hist = histories[backOffLevel]; // efficiency hack: don't need subcat
      hist.clear();
      hist.add(modEvent.modHeadWord().tag());
      break;
    }
    return hist;
  }

  private Event getBaseNPHistory(ModifierEvent modEvent, int backOffLevel) {
    MutableEvent hist = histories[backOffLevel];
    Symbol prevModLabel =
      (modEvent.previousMods().get(0) == startSym ?
       modEvent.head() : modEvent.previousMods().symbolAt(0));
    Word prevModWord =
      (modEvent.previousWords().getWord(0).equals(startWord) ?
       modEvent.headWord() : modEvent.previousWords().getWord(0));
    hist.clear();
    switch (backOffLevel) {
    case 0:
      // for p(w_i | M(t)_i, P, M(w,t)_i-1)
      hist.add(Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.word());
      hist.add(prevModWord.tag());
      break;
    case 1:
      // for p(w_i | M(t)_i, P, M(t)_i-1)
      hist.add(Language.training.removeGapAugmentation(modEvent.modifier()));
      hist.add(modEvent.modHeadWord().tag());
      hist.add(Language.training.removeGapAugmentation(modEvent.parent()));
      hist.add(prevModLabel);
      hist.add(prevModWord.tag());
      break;
    case 2:
      // for p(w_i | t_i)
      hist.add(modEvent.modHeadWord().tag());
      break;
    }
    return hist;
  }

  public Event getFuture(TrainerEvent trainerEvent, int backOffLevel) {
    MutableEvent future = futures[backOffLevel];
    future.clear();
    future.add(((ModifierEvent)trainerEvent).modHeadWord().word());
    return future;
  }

  public ProbabilityStructure copy() {
    return new ModWordModelStructure2();
  }
}
